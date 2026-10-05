package com.ludot.server.app;

import com.ludot.protocol.Speed;

/** Tunable limits and pacing for a room. Production values are the defaults; tests shrink the delays. */
public record SessionConfig(int queueCapacity, long fastMs, long normalMs, long slowMs,
                            int chatMaxLength, int chatHistorySize,
                            int chatRateLimitMessages, long chatRateLimitWindowMs,
                            long emptyRoomCloseMs, int recentLogSize) {

    public static SessionConfig defaults() {
        return new SessionConfig(10_000, Speed.FAST.delayMs(), Speed.NORMAL.delayMs(), Speed.SLOW.delayMs(),
                200, 30, 5, 5_000, 60_000, 50);
    }

    public SessionConfig withQueueCapacity(int capacity) {
        return new SessionConfig(capacity, fastMs, normalMs, slowMs, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, emptyRoomCloseMs, recentLogSize);
    }

    public SessionConfig withDelays(long fast, long normal, long slow) {
        return new SessionConfig(queueCapacity, fast, normal, slow, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, emptyRoomCloseMs, recentLogSize);
    }

    public SessionConfig withEmptyRoomCloseMs(long millis) {
        return new SessionConfig(queueCapacity, fastMs, normalMs, slowMs, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, millis, recentLogSize);
    }

    /** Delay between AI actions at the given speed. */
    public long delayMs(Speed speed) {
        return switch (speed) {
            case FAST -> fastMs;
            case NORMAL -> normalMs;
            case SLOW -> slowMs;
        };
    }
}
