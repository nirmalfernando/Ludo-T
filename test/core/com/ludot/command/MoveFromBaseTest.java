package com.ludot.command;

import com.ludot.TestBoard;
import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MoveFromBase")
class MoveFromBaseTest {

    private Board board;
    private Piece piece;
    @BeforeEach void setUp() {
        board = TestBoard.create();
        piece = new Piece("RED1", Colour.RED, board);
        piece.setDirection(Direction.CLOCKWISE);
    }
        @Test @DisplayName("execute places piece on start cell") void placesOnStartCell() {
        new MoveFromBase(piece).execute();
        assertEquals(board.getStartCell(Colour.RED), piece.getPosition());
    }
    @Test @DisplayName("execute sets piece to OnBoardState") void setsOnBoardState() {
        new MoveFromBase(piece).execute();
        assertInstanceOf(OnBoardState.class, piece.getState());
    }
    @Test @DisplayName("execute returns successful MoveResult") void returnsSuccess() {
        assertTrue(new MoveFromBase(piece).execute().isSuccess());
    }
}