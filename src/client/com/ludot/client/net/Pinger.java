package com.ludot.client.net;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Pings the server on a timer so it does not treat the connection as dead, and times how long each answer takes.
 * That round trip is what the "Connected" badge shows. Sending happens on the timer thread and the Pong arrives on
 * the reader thread, so the one shared field is volatile.
 */
public final class Pinger implements AutoCloseable {

    /** Sends one Ping. */
    public interface Sender {
        void send() throws IOException;
    }

    private final Sender sender;
    private final LongConsumer onRoundTrip;
    private final LongSupplier nanoClock;
    private volatile long sentAtNanos;
    private volatile ScheduledExecutorService timer;

    /** @param onRoundTrip receives the round trip in milliseconds each time a Pong answers a Ping */
    public Pinger(Sender sender, LongConsumer onRoundTrip) {
        this(sender, onRoundTrip, System::nanoTime);
    }

    Pinger(Sender sender, LongConsumer onRoundTrip, LongSupplier nanoClock) {
        this.sender = sender;
        this.onRoundTrip = onRoundTrip;
        this.nanoClock = nanoClock;
    }

    /** Pings now and then every {@code intervalMs}; {@code onFailure} runs if a Ping cannot be sent (the link is dead). */
    public void start(long intervalMs, Runnable onFailure) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "session-ping");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(() -> {
            try {
                ping();
            } catch (IOException e) {
                onFailure.run();
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
        timer = executor;
    }

    void ping() throws IOException {
        sentAtNanos = nanoClock.getAsLong();
        sender.send();
    }

    /** Call when a Pong arrives; only the first Pong after a Ping is timed. */
    public void pongReceived() {
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
