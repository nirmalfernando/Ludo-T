package com.ludot.protocol;

import java.util.Objects;

/** One piece. cell is the track cell (0-51) when on the track; homeStep is the index within the home straight; direction is null at base. */
public record PieceDto(Colour colour, int index, Location location, int cell, int homeStep, Direction direction, EffectType effect, int effectRoundsLeft, int briefingRoundsLeft, boolean inBlock) implements java.io.Serializable {

    public PieceDto {
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(effect, "effect");
    }
}
