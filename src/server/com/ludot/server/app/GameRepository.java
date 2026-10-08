package com.ludot.server.app;

public interface GameRepository {

    GameRepository NONE = operation -> {
        // persistence disabled
    };

    void record(WriteOp operation);
}
