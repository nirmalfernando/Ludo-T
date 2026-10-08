package com.ludot.client.view;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Direction;
import com.ludot.protocol.Location;
import com.ludot.protocol.PieceDto;
import com.ludot.protocol.Variant;

public final class DirectionMark {

    public static final String LEGEND = "↻ clockwise   ↺ counter-clockwise";

    private DirectionMark() {
    }

    public static String glyph(Direction direction) {
        return direction == Direction.CLOCKWISE ? "↻" : "↺";
    }

    public static String words(Direction direction) {
        return direction == Direction.CLOCKWISE ? "clockwise" : "counter-clockwise";
    }

    public static boolean shown(Variant variant, PieceDto piece) {
        boolean onTheLoop = piece.location() == Location.TRACK || piece.location() == Location.BRIEFING;
        return variant == Variant.LUDO_T && piece.direction() != null && onTheLoop;
    }

    // "R1 flips a coin: heads, so it travels clockwise ↻"
    public static String tossMessage(Colour colour, int pieceIndex, boolean clockwise) {
        Direction direction = clockwise ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
        return colour.name().charAt(0) + String.valueOf(pieceIndex + 1) + " flips a coin: " + (clockwise ? "heads" : "tails")
                + ", so it travels " + words(direction) + " " + glyph(direction);
    }
}
