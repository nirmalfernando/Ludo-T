package com.ludot.rules.move;

import com.ludot.TestBoard;
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
@DisplayName("LudoTMoveValidator (T-1)")
class LudoTMoveValidatorTest {

    private Board board;
    @BeforeEach  void setUp()    { board = TestBoard.create(); }
        @Test @DisplayName("heads coin toss gives CLOCKWISE") void headsClockwise() {
        LudoTMoveValidator v = new LudoTMoveValidator(new FixedRandomProvider().addCoins(true));
        assertEquals(Direction.CLOCKWISE, v.assignDirection(new Piece("R1", Colour.RED, board)));
    }
    @Test @DisplayName("tails coin toss gives COUNTER_CLOCKWISE") void tailsCCW() {
        LudoTMoveValidator v = new LudoTMoveValidator(new FixedRandomProvider().addCoins(false));
        assertEquals(Direction.COUNTER_CLOCKWISE, v.assignDirection(new Piece("R1", Colour.RED, board)));
    }
    @Test @DisplayName("CCW piece with exactly 1 pass is still blocked from home straight")
    void onePassStillBlocked() {
        LudoTMoveValidator v = new LudoTMoveValidator(new FixedRandomProvider());
        Piece p = new Piece("R1", Colour.RED, board);
        Cell approachCell = board.getApproachCell(Colour.RED);
        approachCell.addPiece(p);
        p.setPosition(approachCell);
        p.setDirection(Direction.COUNTER_CLOCKWISE);
        p.incrementApproachPassCount(); // exactly 1 — must NOT be enough
        assertFalse(v.canEnterHomeStraight(p),
                "passCount=1 is below the >= 2 threshold and must be rejected");
    }

    @Test @DisplayName("CCW piece requires 2 approach passes for home entry") void ccwRequiresTwoPasses() {
        LudoTMoveValidator v = new LudoTMoveValidator(new FixedRandomProvider());
        Piece p = new Piece("R1", Colour.RED, board);
        Cell approachCell = board.getApproachCell(Colour.RED);
        approachCell.addPiece(p);
        p.setPosition(approachCell);
        p.setDirection(Direction.COUNTER_CLOCKWISE);
        // approachPassCount = 0 — should NOT enter
        assertFalse(v.canEnterHomeStraight(p));
        p.incrementApproachPassCount();
        p.incrementApproachPassCount();
        // approachPassCount = 2 — now can enter
        assertTrue(v.canEnterHomeStraight(p));
    }
}