package com.ludot.protocol;

import java.util.Objects;

/** One narrated game event; text is the Phase 1 formatter's wording. Colour and the cell fields may be null. */
public record EventDto(EventType type, Colour colour, Integer pieceIndex, Integer fromCell, Integer toCell, String text) implements java.io.Serializable {

    public EventDto {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
    }
}
