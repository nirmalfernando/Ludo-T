package com.ludot.model.state;

import com.ludot.model.Board;
import com.ludot.command.MoveFromBase;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("AtBaseState")
class AtBaseStateTest {

    private Board board;
    @Test @DisplayName("handleRoll returns MoveFromBase on 6") void roll6GivesMoveFromBase() {
        Piece p = new Piece("R1", Colour.RED, board);
        var moves = new AtBaseState().handleRoll(p, 6);
        assertEquals(1, moves.size());
        assertInstanceOf(MoveFromBase.class, moves.get(0));
    }
    @Test @DisplayName("handleRoll returns empty for non-6") void nonSixReturnsEmpty() {
        Piece p = new Piece("R1", Colour.RED, board);
        AtBaseState state = new AtBaseState();
        for (int roll = 1; roll <= 5; roll++)
            assertTrue(state.handleRoll(p, roll).isEmpty());
    }
    @Test @DisplayName("canMove returns false") void canMoveIsFalse() {
        assertFalse(new AtBaseState().canMove());
    }
}