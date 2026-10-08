package com.ludot.client;

import com.ludot.client.view.DirectionMark;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Direction;
import com.ludot.protocol.EffectType;
import com.ludot.protocol.Location;
import com.ludot.protocol.PieceDto;
import com.ludot.protocol.Variant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Direction markers: how a piece's travel direction (the coin toss) is shown")
class DirectionMarkTest {

    private static PieceDto piece(Location location, Direction direction) {
        return new PieceDto(Colour.RED, 0, location, location == Location.TRACK ? 5 : 0, 0, direction,
                EffectType.NONE, 0, 0, false);
    }

    @Test @DisplayName("each direction has its own glyph and wording")
    void glyphsAndWords() {
        assertEquals("↻", DirectionMark.glyph(Direction.CLOCKWISE));
        assertEquals("↺", DirectionMark.glyph(Direction.COUNTER_CLOCKWISE));
        assertEquals("clockwise", DirectionMark.words(Direction.CLOCKWISE));
        assertEquals("counter-clockwise", DirectionMark.words(Direction.COUNTER_CLOCKWISE));
    }

    @Test @DisplayName("a marker is shown for a piece out of base in LUDO-T")
    void shownOnTheTrackInLudoT() {
        assertTrue(DirectionMark.shown(Variant.LUDO_T, piece(Location.TRACK, Direction.CLOCKWISE)));
        assertTrue(DirectionMark.shown(Variant.LUDO_T, piece(Location.TRACK, Direction.COUNTER_CLOCKWISE)));
    }

    @Test @DisplayName("no marker for a piece in base (it has no direction yet), without a direction, or in Classic (always clockwise)")
    void notShownWhenThereIsNothingToShow() {
        assertFalse(DirectionMark.shown(Variant.LUDO_T, piece(Location.BASE, null)));
        assertFalse(DirectionMark.shown(Variant.LUDO_T, piece(Location.TRACK, null)));
        assertFalse(DirectionMark.shown(Variant.CLASSIC, piece(Location.TRACK, Direction.CLOCKWISE)));
        assertFalse(DirectionMark.shown(null, piece(Location.TRACK, Direction.CLOCKWISE)));
    }

    @Test @DisplayName("the coin toss message names the piece, the coin face and the direction")
    void tossMessage() {
        assertEquals("R1 flips a coin: heads, so it travels clockwise ↻",
                DirectionMark.tossMessage(Colour.RED, 0, true));
        assertEquals("B3 flips a coin: tails, so it travels counter-clockwise ↺",
                DirectionMark.tossMessage(Colour.BLUE, 2, false));
    }

    @Test @DisplayName("the legend explains both glyphs")
    void legend() {
        assertTrue(DirectionMark.LEGEND.contains("↻") && DirectionMark.LEGEND.contains("↺"));
    }
}
