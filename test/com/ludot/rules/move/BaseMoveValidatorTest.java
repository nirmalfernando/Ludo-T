package com.ludot.rules.move;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BaseMoveValidator")
class BaseMoveValidatorTest {
    private BaseMoveValidator validator;
    private Piece piece;
    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        validator = new BaseMoveValidator(new FixedRandomProvider()) {};
        piece = new Piece("R1", Colour.RED);
        Cell start = Board.getInstance().getCell(10);
        start.addPiece(piece);
        piece.setPosition(start);
        piece.setState(new OnBoardState());
        piece.setDirection(Direction.CLOCKWISE);
    }
    @AfterEach void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("CW destination uses modulo arithmetic") void cwDestination() {
        assertEquals(13, validator.getDestinationCell(piece, 3).get().getId()); // 10+3=13
    }
    @Test @DisplayName("empty Optional for piece at base") void emptyForBasePiece() {
        assertTrue(validator.getDestinationCell(new Piece("R2", Colour.RED), 3).isEmpty());
    }
    @Test @DisplayName("assignDirection always returns CLOCKWISE") void alwaysClockwise() {
        assertEquals(Direction.CLOCKWISE, validator.assignDirection(piece));
    }

    @Test @DisplayName("CW destination wraps correctly at cell 51")
    void cwWrapsAtCell51() {
        // piece at cell 51, CW 3 steps: (51 + 3) % 52 = 2
        Cell c51 = Board.getInstance().getCell(51);
        piece.getPosition().removePiece(piece);
        c51.addPiece(piece); piece.setPosition(c51);
        assertEquals(2, validator.getDestinationCell(piece, 3).get().getId());
    }

    @Test @DisplayName("CCW destination wraps correctly at cell 0")
    void ccwWrapsAtCellZero() {
        // piece at cell 2, CCW 3 steps: (2 - 3 + 52) % 52 = 51
        Cell c2 = Board.getInstance().getCell(2);
        piece.getPosition().removePiece(piece);
        c2.addPiece(piece); piece.setPosition(c2);
        piece.setDirection(Direction.COUNTER_CLOCKWISE);
        assertEquals(51, validator.getDestinationCell(piece, 3).get().getId());
    }
}