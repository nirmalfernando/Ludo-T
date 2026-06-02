package com.ludot.rules.move;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.factory.CellFactory;
import com.ludot.model.*;
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
        assertEquals(13, validator.getDestinationCell(piece, 3).getId()); // 10+3=13
    }
    @Test @DisplayName("null for piece at base") void nullForBasePiece() {
        assertNull(validator.getDestinationCell(new Piece("R2", Colour.RED), 3));
    }
    @Test @DisplayName("assignDirection always returns CLOCKWISE") void alwaysClockwise() {
        assertEquals(Direction.CLOCKWISE, validator.assignDirection(piece));
    }
}