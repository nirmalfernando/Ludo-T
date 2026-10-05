package com.ludot.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StateHash")
class StateHashTest {

    private static PieceDto piece(Colour colour, int index, int cell) {
        return new PieceDto(colour, index, Location.TRACK, cell, 0, Direction.CLOCKWISE,
                EffectType.NONE, 0, 0, false);
    }

    private static BoardDto board(int cell) {
        return new BoardDto(List.of(piece(Colour.RED, 0, cell), piece(Colour.BLUE, 1, 5)),
                List.of(new MysteryCellDto(7, 2)), Map.of(Colour.RED, 1), 3);
    }

    @Test @DisplayName("the same state always gives the same hash")
    void deterministic() {
        assertEquals(StateHash.of(board(10), 4), StateHash.of(board(10), 4));
    }

    @Test @DisplayName("piece order in the list does not matter")
    void orderIndependent() {
        List<PieceDto> reversed = new ArrayList<>(board(10).pieces());
        java.util.Collections.reverse(reversed);
        BoardDto other = new BoardDto(reversed, board(10).mysteryCells(), board(10).captureCounts(), 3);
        assertEquals(StateHash.of(board(10), 4), StateHash.of(other, 4));
    }

    @Test @DisplayName("moving a piece changes the hash")
    void pieceChangeDetected() {
        assertNotEquals(StateHash.of(board(10), 4), StateHash.of(board(11), 4));
    }

    @Test @DisplayName("a different turn number changes the hash")
    void turnChangeDetected() {
        assertNotEquals(StateHash.of(board(10), 4), StateHash.of(board(10), 5));
    }

    @Test @DisplayName("a different capture count or mystery state changes the hash")
    void otherStateDetected() {
        BoardDto base = board(10);
        BoardDto moreCaptures = new BoardDto(base.pieces(), base.mysteryCells(), Map.of(Colour.RED, 2), 3);
        BoardDto noMystery = new BoardDto(base.pieces(), List.of(), base.captureCounts(), 3);
        assertNotEquals(StateHash.of(base, 4), StateHash.of(moreCaptures, 4));
        assertNotEquals(StateHash.of(base, 4), StateHash.of(noMystery, 4));
    }
}
