package com.ludot.model.state;

import com.ludot.model.Board;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("OnBoardState")
class OnBoardStateTest {

    private Board board;
    @Test @DisplayName("handleRoll returns MoveOnBoard with effective steps") void returnsMoveOnBoard() {
        Piece p = new Piece("R1", Colour.RED, board);
        var moves = new OnBoardState().handleRoll(p, 4);
        assertEquals(1, moves.size());
        assertInstanceOf(MoveOnBoard.class, moves.get(0));
        assertEquals(4, ((MoveOnBoard) moves.get(0)).getSteps());
    }
    @Test @DisplayName("canMove returns true") void canMoveIsTrue() {
        assertTrue(new OnBoardState().canMove());
    }
}