package com.ludot.client.net;

import java.util.function.LongSupplier;

// generic so the retry rules can be tested without a network
public final class Reconnector<T> {

    public interface Attempt<T> {
        T open() throws Exception;
    }

    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    // what came of it: the new connection, or why it was given up
    public record Result<T>(T value, String failure) {
        public boolean succeeded() {
            return value != null;
        }
    }

    private final long[] backoffMs;
    private final long giveUpAfterMs;
    private final Sleeper sleeper;
    private final LongSupplier clock;
    private volatile boolean cancelled;

    public Reconnector(long[] backoffMs, long giveUpAfterMs) {
        this(backoffMs, giveUpAfterMs, Thread::sleep, System::currentTimeMillis);
    }

    Reconnector(long[] backoffMs, long giveUpAfterMs, Sleeper sleeper, LongSupplier clock) {
        this.backoffMs = backoffMs.clone();
        this.giveUpAfterMs = giveUpAfterMs;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    public void cancel() {
        cancelled = true;
    }

    public Result<T> run(Attempt<T> attempt) {
        long started = clock.getAsLong();
        String lastError = "no answer";
        for (int tries = 0; !cancelled; tries++) {
            try {
                return new Result<>(attempt.open(), null);
            } catch (SessionExpiredException e) {
                return new Result<>(null, "Your session has expired: please sign in again");
            } catch (Exception e) {
                lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            long delay = backoffMs[Math.min(tries, backoffMs.length - 1)];
            if (clock.getAsLong() - started + delay > giveUpAfterMs || !pause(delay)) {
                break;
            }
        }
        return new Result<>(null, cancelled ? "Reconnecting was cancelled" : "Could not reconnect (" + lastError + ")");
    }

    private boolean pause(long millis) {
        try {
            sleeper.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
