package com.ludot.client.state;

public final class ClockSync {

    private volatile long offsetMs;

    public void update(long serverMs, long localMs) {
        offsetMs = serverMs - localMs;
    }

    public long serverNow(long localMs) {
        return localMs + offsetMs;
    }

    public long remainingMs(long deadlineServerMs, long localMs) {
        return deadlineServerMs - serverNow(localMs);
    }

    // "0:23" style text for a countdown; never negative
    public static String format(long remainingMs) {
        long seconds = Math.max(0, (remainingMs + 999) / 1_000);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
