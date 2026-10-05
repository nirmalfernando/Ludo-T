package com.ludot.model.state;

import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("AtHomeState")
class AtHomeStateTest {

    private Board board;
    @Test @DisplayName("handleRoll always returns empty") void handleRollEmpty() {
        Piece p = new Piece("R1", Colour.RED, board);
        for (int roll = 1; roll <= 6; roll++)
            assertTrue(new AtHomeState().handleRoll(p, roll).isEmpty());
    }
    @Test @DisplayName("canMove returns false") void canMoveIsFalse() {
        assertFalse(new AtHomeState().canMove());
    }
}