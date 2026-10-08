package com.ludot.protocol;

import java.util.Objects;

// playerId and nickname are null unless a person holds it
public record SeatDto(Colour colour, Controller controller, String playerId, String nickname, boolean connected) implements java.io.Serializable {

    public SeatDto {
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(controller, "controller");
    }
}
