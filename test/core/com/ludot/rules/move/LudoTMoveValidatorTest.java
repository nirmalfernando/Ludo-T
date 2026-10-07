package com.ludot.rules.move;

import com.ludot.TestBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
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
    @Test @DisplayName("LUDO-T uses a coin toss, Classic does not") void coinTossOnlyInLudoT() {
        assertTrue(new LudoTMoveValidator(new FixedRandomProvider()).usesCoinToss());
    }
}
