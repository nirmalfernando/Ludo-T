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
@DisplayName("LudoTMoveValidator (T-1)")
class LudoTMoveValidatorTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); CellFactory.initializeBoard(Board.getInstance()); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("heads coin toss gives CLOCKWISE") void headsClockwise() {
        LudoTMoveValidator v = new LudoTMoveValidator(new FixedRandomProvider().addCoins(true));
        assertEquals(Direction.CLOCKWISE, v.assignDirection(new Piece("R1", Colour.RED)));
    }
    @Test @DisplayName("tails coin toss gives COUNTER_CLOCKWISE") void tailsCCW() {
        LudoTMoveValidator v = new LudoTMoveValidator(new FixedRandomProvider().addCoins(false));
        assertEquals(Direction.COUNTER_CLOCKWISE, v.assignDirection(new Piece("R1", Colour.RED)));
    }
    @Test @DisplayName("CCW piece requires 2 approach passes for home entry") void ccwRequiresTwoPasses() {
        LudoTMoveValidator v = new LudoTMoveValidator(new FixedRandomProvider());
        Piece p = new Piece("R1", Colour.RED);
        Cell approachCell = Board.getInstance().getApproachCell(Colour.RED);
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