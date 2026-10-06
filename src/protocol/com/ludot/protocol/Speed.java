package com.ludot.protocol;

/** Pacing between AI actions, in milliseconds. */
public enum Speed {
    FAST(400),
    NORMAL(1000),
    SLOW(2000);

    private final long delayMs;

    Speed(long delayMs) {
        this.delayMs = delayMs;
    }

    public long delayMs() {
        return delayMs;
    }
}
