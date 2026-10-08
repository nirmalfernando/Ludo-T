package com.ludot.client.net;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

// pings the server on a timer so it does not treat the connection as dead, and times how long each answer takes
public final class Pinger implements AutoCloseable {

    public interface Sender {
        void send() throws IOException;
    }

    private final Sender sender;
    private final LongConsumer onRoundTrip;
    private final LongSupplier nanoClock;
    private volatile long sentAtNanos;
    private volatile long lastPongNanos;
    private volatile ScheduledExecutorService timer;

    public Pinger(Sender sender, LongConsumer onRoundTrip) {
        this(sender, onRoundTrip, System::nanoTime);
    }

    Pinger(Sender sender, LongConsumer onRoundTrip, LongSupplier nanoClock) {
        this.sender = sender;
        this.onRoundTrip = onRoundTrip;
        this.nanoClock = nanoClock;
        this.lastPongNanos = nanoClock.getAsLong();
    }

    public void start(long intervalMs, long staleAfterMs, Runnable onFailure) {
        lastPongNanos = nanoClock.getAsLong();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "session-ping");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(() -> {
            try {
                tick(staleAfterMs);
            } catch (IOException e) {
                onFailure.run();
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
        timer = executor;
    }

    // one timer beat: complain if the server has gone quiet, otherwise ping
    void tick(long staleAfterMs) throws IOException {
        long quietMs = (nanoClock.getAsLong() - lastPongNanos) / 1_000_000;
        if (quietMs > staleAfterMs) {
            throw new IOException("No answer from the server for " + quietMs / 1_000 + " s");
        }
        ping();
    }

    void ping() throws IOException {
        sentAtNanos = nanoClock.getAsLong();
        sender.send();
    }

    public void pongReceived() {
        lastPongNanos = nanoClock.getAsLong();
        long sent = sentAtNanos;
        if (sent != 0) {
            sentAtNanos = 0;
            onRoundTrip.accept((nanoClock.getAsLong() - sent) / 1_000_000);
        }
    }

    @Override
    public void close() {
        ScheduledExecutorService executor = timer;
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
