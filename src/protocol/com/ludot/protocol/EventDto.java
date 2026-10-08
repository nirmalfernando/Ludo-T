package com.ludot.protocol;

import java.util.Objects;

public record EventDto(EventType type, Colour colour, Integer pieceIndex, Integer fromCell, Integer toCell, String text, Integer value) implements java.io.Serializable {

    public EventDto {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
    }

    public EventDto(EventType type, Colour colour, Integer pieceIndex, Integer fromCell, Integer toCell, String text) {
        this(type, colour, pieceIndex, fromCell, toCell, text, null);
    }
}
