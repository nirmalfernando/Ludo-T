package com.ludot.command;
import com.ludot.factory.CellFactory;
import com.ludot.model.*;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MoveFromBase")
class MoveFromBaseTest {
    private Piece piece;
    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        piece = new Piece("RED1", Colour.RED);
        piece.setDirection(Direction.CLOCKWISE);
    }
    @AfterEach void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("execute places piece on start cell") void placesOnStartCell() {
        new MoveFromBase(piece).execute();
        assertEquals(Board.getInstance().getStartCell(Colour.RED), piece.getPosition());
    }
    @Test @DisplayName("execute sets piece to OnBoardState") void setsOnBoardState() {
        new MoveFromBase(piece).execute();
        assertInstanceOf(OnBoardState.class, piece.getState());
    }
    @Test @DisplayName("execute returns successful MoveResult") void returnsSuccess() {
        assertTrue(new MoveFromBase(piece).execute().isSuccess());
    }
}