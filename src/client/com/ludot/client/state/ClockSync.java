package com.ludot.client.state;

/**
 * Keeps a countdown honest when the client's clock differs from the server's. Deadlines are server times, so
 * each Pong (which carries the server's clock) tells us how far ahead or behind we are. The error left is about
 * half a network round trip, far smaller than a second.
 */
public final class ClockSync {

    private volatile long offsetMs;

    /** Records that the server's clock read {@code serverMs} at the moment ours read {@code localMs}. */
    public void update(long serverMs, long localMs) {
        offsetMs = serverMs - localMs;
    }

    /** The server's time now, estimated from our own clock. */
    public long serverNow(long localMs) {
        return localMs + offsetMs;
    }

    /** Milliseconds until a server-time deadline (negative once it has passed). */
    public long remainingMs(long deadlineServerMs, long localMs) {
        return deadlineServerMs - serverNow(localMs);
    }

    /** "0:23" style text for a countdown; never negative. */
    public static String format(long remainingMs) {
        long seconds = Math.max(0, (remainingMs + 999) / 1_000);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
