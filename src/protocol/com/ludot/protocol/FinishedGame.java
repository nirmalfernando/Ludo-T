package com.ludot.protocol;

import java.util.Objects;

public record FinishedGame(String gameId, RoomMode mode, Variant variant, long seed, Colour winner, java.time.Instant finishedAt, int turns) implements java.io.Serializable {

    public FinishedGame {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(finishedAt, "finishedAt");
    }
}
