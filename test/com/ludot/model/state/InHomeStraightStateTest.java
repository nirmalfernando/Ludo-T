package com.ludot.model.state;
import com.ludot.command.MoveOnBoard;
import com.ludot.factory.CellFactory;
import com.ludot.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("InHomeStraightState")
class InHomeStraightStateTest {
    private Piece piece;
    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        piece = new Piece("R1", Colour.RED);
        // place on first home-straight cell
        List<Cell> hs = Board.getInstance().getHomeStraight(Colour.RED);
        hs.get(0).addPiece(piece);
        piece.setPosition(hs.get(0));
        piece.setState(new InHomeStraightState());
    }
    @AfterEach void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("handleRoll returns MoveOnBoard when steps fit") void moveFits() {
        var moves = new InHomeStraightState().handleRoll(piece, 2);
        assertEquals(1, moves.size());
        assertInstanceOf(MoveOnBoard.class, moves.get(0));
    }
    @Test @DisplayName("handleRoll returns empty on overshoot") void overshootReturnsEmpty() {
        // HOME_STRAIGHT has 5 cells (index 0-4); from index 0, steps=5 would overshoot
        var moves = new InHomeStraightState().handleRoll(piece, 5);
        assertTrue(moves.isEmpty());
    }
    @Test @DisplayName("canMove returns true") void canMoveIsTrue() {
        assertTrue(new InHomeStraightState().canMove());
    }
}