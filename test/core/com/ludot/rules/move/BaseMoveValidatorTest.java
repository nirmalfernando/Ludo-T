package com.ludot.rules.move;

import com.ludot.TestBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BaseMoveValidator")
class BaseMoveValidatorTest {

    private Board board;
    private BaseMoveValidator validator;
    private Piece piece;
    @BeforeEach void setUp() {
        board = TestBoard.create();
        validator = new BaseMoveValidator(new FixedRandomProvider()) {};
        piece = new Piece("R1", Colour.RED, board);
        Cell start = board.getCell(10);
        start.addPiece(piece);
        piece.setPosition(start);
        piece.setState(new OnBoardState());
        piece.setDirection(Direction.CLOCKWISE);
    }
    @Test @DisplayName("Classic has no coin toss") void noCoinToss() {
        assertFalse(validator.usesCoinToss());
    }
    @Test @DisplayName("assignDirection always returns CLOCKWISE") void alwaysClockwise() {
        assertEquals(Direction.CLOCKWISE, validator.assignDirection(piece));
    }
}
