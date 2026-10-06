package com.ludot.server.adapters.persistence;

import com.ludot.server.app.BatchWriter;
import com.ludot.server.app.GameRepository;
import com.ludot.server.app.PersistenceException;
import com.ludot.server.app.WriteOp;
import com.ludot.server.concurrent.NamedThreadFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorator that makes persistence asynchronous. {@link #record} only puts the operation on a queue, so
 * a room thread never waits for the network; a single "db-writer" thread drains the queue into batches
 * and writes each as one transaction. One writer and a FIFO queue keep every room's writes in order.
 *
 * <p>A failed batch is retried with backoff; if it still fails, its operations are tried one by one so a
 * single bad row cannot take good rows down with it. Whatever still fails is logged and counted:
 * durability is traded for availability, because a game must never stall on its log.
 */
public final class AsyncGameRepository implements GameRepository, AutoCloseable {

    /** Counters for logging and tests. */
    public record Stats(long written, long dropped, long batches, long retries, int queueDepth, int maxQueueDepth) {
    }

    private static final Logger LOG = Logger.getLogger(AsyncGameRepository.class.getName());
    private static final int DEFAULT_MAX_BATCH = 50;
    private static final int BACKLOG_WARNING = 1_000;
    private static final long[] DEFAULT_RETRY_DELAYS_MS = {100, 400, 1_600};
    private static final long CLOSE_TIMEOUT_MS = 10_000;

    private final BatchWriter writer;
    private final int maxBatch;
    private final long[] retryDelaysMs;
    private final LinkedBlockingQueue<WriteOp> queue = new LinkedBlockingQueue<>();
    private final Thread thread;
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private volatile int maxQueueDepth;
    private volatile boolean closing;
    private volatile boolean backlogWarned;

    public AsyncGameRepository(BatchWriter writer) {
        this(writer, DEFAULT_MAX_BATCH, DEFAULT_RETRY_DELAYS_MS);
    }

    public AsyncGameRepository(BatchWriter writer, int maxBatch, long[] retryDelaysMs) {
        this.writer = writer;
        this.maxBatch = maxBatch;
        this.retryDelaysMs = retryDelaysMs.clone();
        this.thread = new NamedThreadFactory("db-writer", false).newThread(this::writeLoop);
        this.thread.start();
    }

    /** Queues the operation and returns at once. After {@link #close} new operations are refused and counted as dropped. */
    @Override
    public void record(WriteOp operation) {
        if (closing) {
            dropped.incrementAndGet();
            return;
        }
        queue.add(operation);
        int depth = queue.size();
        if (depth > maxQueueDepth) {
            maxQueueDepth = depth;
        }
        if (depth > BACKLOG_WARNING && !backlogWarned) {
            backlogWarned = true;
            LOG.warning("Database write queue has " + depth + " operations waiting; is the database slow?");
        }
    }

    public Stats stats() {
        return new Stats(written.get(), dropped.get(), batches.get(), retries.get(), queue.size(), maxQueueDepth);
    }

    /** Waits until everything queued so far has been written (or given up on). For tests and tools. */
    public boolean awaitDrained(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (queue.isEmpty() && inFlight.get() == 0) {
                return true;
            }
            Thread.sleep(2);
        }
        return queue.isEmpty() && inFlight.get() == 0;
    }

    private final java.util.concurrent.atomic.AtomicInteger inFlight = new java.util.concurrent.atomic.AtomicInteger();

    private void writeLoop() {
        try {
            while (true) {
                WriteOp first = queue.poll(200, TimeUnit.MILLISECONDS);
                if (first == null) {
                    if (closing) {
                        return;
                    }
                    continue;
                }
                List<WriteOp> batch = new ArrayList<>();
                batch.add(first);
                inFlight.set(1);
                queue.drainTo(batch, maxBatch - 1);
                inFlight.set(batch.size());
                write(batch);
                inFlight.set(0);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void write(List<WriteOp> batch) throws InterruptedException {
        batches.incrementAndGet();
        PersistenceException last = null;
        for (int attempt = 0; attempt <= retryDelaysMs.length; attempt++) {
            try {
                writer.writeAll(batch);
                written.addAndGet(batch.size());
                return;
            } catch (PersistenceException e) {
                last = e;
                if (attempt < retryDelaysMs.length) {
                    retries.incrementAndGet();
                    LOG.log(Level.WARNING, "Database write failed (attempt " + (attempt + 1) + "), retrying: "
                            + e.getMessage());
                    Thread.sleep(retryDelaysMs[attempt]);
                }
            }
        }
        LOG.log(Level.SEVERE, "Batch of " + batch.size() + " operations failed after retries; trying them one by one", last);
        writeOneByOne(batch);
    }

    // a single bad row must not take the good ones down with it
    private void writeOneByOne(List<WriteOp> batch) {
        for (WriteOp operation : batch) {
            try {
                writer.writeAll(List.of(operation));
                written.incrementAndGet();
            } catch (PersistenceException e) {
                dropped.incrementAndGet();
                LOG.log(Level.SEVERE, "Dropping " + operation + ": " + e.getMessage());
            }
        }
    }

    /** Stops accepting operations, writes whatever is queued (up to ten seconds) and stops the writer thread. */
    @Override
    public void close() {
        closing = true;
        try {
            thread.join(CLOSE_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            LOG.warning("Database writer did not finish in time; " + queue.size() + " operations were left unwritten");
            thread.interrupt();
        }
    }
}
