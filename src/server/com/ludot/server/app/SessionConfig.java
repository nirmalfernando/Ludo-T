package com.ludot.server.app;

import com.ludot.protocol.Speed;

/**
 * Tunable limits and pacing for a room. Production values are the defaults; tests shrink the delays.
 * A turnTimeoutMs of 0 or less switches turn timers off.
 */
public record SessionConfig(int queueCapacity, long fastMs, long normalMs, long slowMs,
                            int chatMaxLength, int chatHistorySize,
                            int chatRateLimitMessages, long chatRateLimitWindowMs,
                            long emptyRoomCloseMs, int recentLogSize, int requestMemory,
                            long turnTimeoutMs, int afkAfterTimeouts) {

    /**
     * How many recent answers a room remembers per player to replay on a repeated request id. It must be
     * larger than the number of requests that can be queued between a request and its retry, or a retry
     * under a flood is evaluated again instead of answered from memory.
     */
    public static final int DEFAULT_REQUEST_MEMORY = 1_024;

    /** How long a person has to roll or to choose a move before the AI does it for them. */
    public static final long DEFAULT_TURN_TIMEOUT_MS = 30_000;

    /** Consecutive missed turns after which a person's seat is handed to the AI until they take it back. */
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

    /** Delay between AI actions at the given speed. */
    public long delayMs(Speed speed) {
        return switch (speed) {
            case FAST -> fastMs;
            case NORMAL -> normalMs;
            case SLOW -> slowMs;
        };
    }
}
