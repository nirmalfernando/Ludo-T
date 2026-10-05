package com.ludot.protocol;

/** Pacing between AI actions, in milliseconds. */
public enum Speed {
    FAST(150),
    NORMAL(700),
    SLOW(1500);

    private final long delayMs;

    Speed(long delayMs) {
        this.delayMs = delayMs;
    }

    public long delayMs() {
        return delayMs;
    }
}
