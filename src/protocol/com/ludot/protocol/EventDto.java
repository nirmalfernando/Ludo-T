package com.ludot.protocol;

import java.util.Objects;

/**
 * One narrated game event; text is the Phase 1 formatter's wording. Colour and the cell fields may be null. Value is a
 * number some events carry so that a client need not read the text: the die for ROLL_OFF, and for COIN_TOSS 1 when the
 * piece will travel clockwise and 0 when counter-clockwise.
 */
public record EventDto(EventType type, Colour colour, Integer pieceIndex, Integer fromCell, Integer toCell, String text, Integer value) implements java.io.Serializable {

    public EventDto {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
    }

    /** An event that carries no number. */
    public EventDto(EventType type, Colour colour, Integer pieceIndex, Integer fromCell, Integer toCell, String text) {
        this(type, colour, pieceIndex, fromCell, toCell, text, null);
    }
}
