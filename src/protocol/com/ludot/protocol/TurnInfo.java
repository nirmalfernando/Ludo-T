package com.ludot.protocol;

import java.util.Objects;

/** Whose turn it is and in which phase; lastRoll and deadlineEpochMs may be null. */
public record TurnInfo(int turnNumber, Colour colour, Phase phase, Integer lastRoll, Long deadlineEpochMs) implements java.io.Serializable {

    public TurnInfo {
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(phase, "phase");
    }
}
