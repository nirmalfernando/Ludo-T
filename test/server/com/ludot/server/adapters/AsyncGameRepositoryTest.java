package com.ludot.server.adapters;

import com.ludot.protocol.Colour;
import com.ludot.server.adapters.persistence.AsyncGameRepository;
import com.ludot.server.app.BatchWriter;
import com.ludot.server.app.DecisionRecord;
import com.ludot.server.app.DecisionSource;
import com.ludot.server.app.PersistenceException;
import com.ludot.server.app.PlayerRecord;
import com.ludot.server.app.WriteOp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AsyncGameRepository")
class AsyncGameRepositoryTest {

    private static final long[] FAST_RETRIES = {5, 5, 5};

    /** A writer that remembers each batch, and can be made slow or to fail on demand. */
    private static final class FakeWriter implements BatchWriter {
        final List<List<WriteOp>> batches = Collections.synchronizedList(new ArrayList<>());
        volatile long delayMs;
        volatile int failuresLeft;
        volatile WriteOp poison;
        volatile CountDownLatch gate;

        @Override
        public void writeAll(List<WriteOp> operations) throws PersistenceException {
            CountDownLatch latch = gate;
            if (latch != null) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (failuresLeft > 0) {
                failuresLeft--;
                throw new PersistenceException("simulated outage", null);
            }
            if (poison != null && operations.contains(poison)) {
                throw new PersistenceException("simulated bad row", null);
            }
            batches.add(List.copyOf(operations));
        }

        List<WriteOp> everything() {
            List<WriteOp> all = new ArrayList<>();
            synchronized (batches) {
                batches.forEach(all::addAll);
            }
            return all;
        }
    }

    private static DecisionRecord decision(int turn) {
        return new DecisionRecord("R-0001", turn, Colour.RED, 3, 0, DecisionSource.AI);
    }

    @Test @DisplayName("operations are written in exactly the order they were recorded")
    void keepsOrder() throws Exception {
        FakeWriter writer = new FakeWriter();
        try (AsyncGameRepository repository = new AsyncGameRepository(writer, 50, FAST_RETRIES)) {
            for (int i = 0; i < 500; i++) {
                repository.record(decision(i));
            }
            assertTrue(repository.awaitDrained(5000));
        }
        List<WriteOp> all = writer.everything();
        assertEquals(500, all.size());
        for (int i = 0; i < 500; i++) {
            assertEquals(i, ((DecisionRecord) all.get(i)).turnNumber());
        }
    }

    @Test @DisplayName("a backlog is drained in batches of at most the limit, so many writes cost few round trips")
    void batches() throws Exception {
        FakeWriter writer = new FakeWriter();
        writer.gate = new CountDownLatch(1);
        AsyncGameRepository repository = new AsyncGameRepository(writer, 50, FAST_RETRIES);
        for (int i = 0; i < 400; i++) {
            repository.record(decision(i));
        }
        writer.gate.countDown();
        assertTrue(repository.awaitDrained(5000));
        repository.close();
        assertTrue(writer.batches.stream().allMatch(batch -> batch.size() <= 50));
        assertTrue(writer.batches.size() < 400 / 2, "batching should need far fewer writes than operations, got "
                + writer.batches.size());
        assertEquals(400, repository.stats().written());
    }

    @Test @DisplayName("record() returns at once even when the database is very slow")
    void recordNeverBlocks() throws Exception {
        FakeWriter writer = new FakeWriter();
        writer.delayMs = 300;
        try (AsyncGameRepository repository = new AsyncGameRepository(writer, 50, FAST_RETRIES)) {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                repository.record(decision(i));
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsedMs < 200, "1000 records took " + elapsedMs + " ms while the database took 300 ms per write");
            assertTrue(repository.awaitDrained(20_000));
            assertEquals(1000, repository.stats().written());
        }
    }

    @Test @DisplayName("a transient outage is retried and nothing is lost")
    void retriesTransientFailure() throws Exception {
        FakeWriter writer = new FakeWriter();
        writer.failuresLeft = 2;
        try (AsyncGameRepository repository = new AsyncGameRepository(writer, 50, FAST_RETRIES)) {
            repository.record(decision(1));
            assertTrue(repository.awaitDrained(5000));
            assertEquals(2, repository.stats().retries());
            assertEquals(1, repository.stats().written());
            assertEquals(0, repository.stats().dropped());
        }
        assertEquals(List.of(decision(1)), writer.everything());
    }

    @Test @DisplayName("one bad row is dropped after retries and the good rows in its batch are still written")
    void badRowDoesNotPoisonTheBatch() throws Exception {
        FakeWriter writer = new FakeWriter();
        writer.gate = new CountDownLatch(1);
        writer.poison = decision(2);
        AsyncGameRepository repository = new AsyncGameRepository(writer, 50, FAST_RETRIES);
        for (int i = 0; i < 5; i++) {
            repository.record(decision(i));
        }
        writer.gate.countDown();
        assertTrue(repository.awaitDrained(5000));
        repository.close();
        List<WriteOp> written = writer.everything();
        assertEquals(4, written.size());
        assertFalse(written.contains(decision(2)));
        assertEquals(1, repository.stats().dropped());
        assertEquals(4, repository.stats().written());
    }

    @Test @DisplayName("close writes everything still queued, and later records are refused and counted")
    void closeDrains() throws Exception {
        FakeWriter writer = new FakeWriter();
        writer.delayMs = 20;
        AsyncGameRepository repository = new AsyncGameRepository(writer, 10, FAST_RETRIES);
        for (int i = 0; i < 100; i++) {
            repository.record(decision(i));
        }
        repository.close();
        assertEquals(100, writer.everything().size());
        repository.record(decision(1000));
        assertEquals(1, repository.stats().dropped());
    }

    @Test @DisplayName("records from many threads are all written, and each thread's own order is kept")
    void concurrentProducers() throws Exception {
        FakeWriter writer = new FakeWriter();
        AtomicInteger started = new AtomicInteger();
        try (AsyncGameRepository repository = new AsyncGameRepository(writer, 50, FAST_RETRIES)) {
            Thread[] threads = new Thread[8];
            for (int t = 0; t < threads.length; t++) {
                int id = t;
                threads[t] = new Thread(() -> {
                    started.incrementAndGet();
                    for (int i = 0; i < 250; i++) {
                        repository.record(new PlayerRecord("t" + id, String.valueOf(i)));
                    }
                });
                threads[t].start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
            assertTrue(repository.awaitDrained(10_000));
        }
        List<WriteOp> all = writer.everything();
        assertEquals(2000, all.size());
        int[] last = new int[8];
        java.util.Arrays.fill(last, -1);
        for (WriteOp op : all) {
            PlayerRecord record = (PlayerRecord) op;
            int thread = Integer.parseInt(record.id().substring(1));
            int sequence = Integer.parseInt(record.nickname());
            assertTrue(sequence > last[thread], "thread " + thread + " went backwards");
            last[thread] = sequence;
        }
    }
}
