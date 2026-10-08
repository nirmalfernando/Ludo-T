package com.ludot.protocol;

import java.util.Objects;

public record TurnInfo(int turnNumber, Colour colour, Phase phase, Integer lastRoll, Long deadlineEpochMs) implements java.io.Serializable {

    public TurnInfo {
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(phase, "phase");
    }
}
