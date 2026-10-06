package com.ludot.server.app;

/**
 * Port for persistence. Implementations must not block the caller for long: rooms call this inline on
 * their own thread, so a real database is reached through an asynchronous decorator that only queues.
 */
public interface GameRepository {

    GameRepository NONE = operation -> {
        // persistence disabled
    };

    void record(WriteOp operation);
}
