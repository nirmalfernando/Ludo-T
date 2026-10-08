package com.ludot.server.app;

import com.ludot.protocol.Speed;

public record SessionConfig(int queueCapacity, long fastMs, long normalMs, long slowMs,
                            int chatMaxLength, int chatHistorySize,
                            int chatRateLimitMessages, long chatRateLimitWindowMs,
                            long emptyRoomCloseMs, int recentLogSize, int requestMemory,
                            long turnTimeoutMs, int afkAfterTimeouts) {

    public static final int DEFAULT_REQUEST_MEMORY = 1_024;

    public static final long DEFAULT_TURN_TIMEOUT_MS = 30_000;

    public static final int DEFAULT_AFK_AFTER_TIMEOUTS = 2;

    public static SessionConfig defaults() {
        return new SessionConfig(10_000, Speed.FAST.delayMs(), Speed.NORMAL.delayMs(), Speed.SLOW.delayMs(),
                200, 30, 5, 5_000, 60_000, 50, DEFAULT_REQUEST_MEMORY,
                DEFAULT_TURN_TIMEOUT_MS, DEFAULT_AFK_AFTER_TIMEOUTS);
    }

    public SessionConfig withQueueCapacity(int capacity) {
        return new SessionConfig(capacity, fastMs, normalMs, slowMs, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, emptyRoomCloseMs, recentLogSize, requestMemory,
                turnTimeoutMs, afkAfterTimeouts);
    }

    public SessionConfig withDelays(long fast, long normal, long slow) {
        return new SessionConfig(queueCapacity, fast, normal, slow, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, emptyRoomCloseMs, recentLogSize, requestMemory,
                turnTimeoutMs, afkAfterTimeouts);
    }

    public SessionConfig withEmptyRoomCloseMs(long millis) {
        return new SessionConfig(queueCapacity, fastMs, normalMs, slowMs, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, millis, recentLogSize, requestMemory,
                turnTimeoutMs, afkAfterTimeouts);
    }

    public SessionConfig withRequestMemory(int memory) {
        return new SessionConfig(queueCapacity, fastMs, normalMs, slowMs, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, emptyRoomCloseMs, recentLogSize, memory,
                turnTimeoutMs, afkAfterTimeouts);
    }

    public SessionConfig withTurnTimeoutMs(long millis) {
        return new SessionConfig(queueCapacity, fastMs, normalMs, slowMs, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, emptyRoomCloseMs, recentLogSize, requestMemory,
                millis, afkAfterTimeouts);
    }

    public SessionConfig withAfkAfterTimeouts(int timeouts) {
        return new SessionConfig(queueCapacity, fastMs, normalMs, slowMs, chatMaxLength, chatHistorySize,
                chatRateLimitMessages, chatRateLimitWindowMs, emptyRoomCloseMs, recentLogSize, requestMemory,
                turnTimeoutMs, timeouts);
    }

    public long delayMs(Speed speed) {
        return switch (speed) {
            case FAST -> fastMs;
            case NORMAL -> normalMs;
            case SLOW -> slowMs;
        };
    }
}
