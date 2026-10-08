package com.ludot.rules.capture;

import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.AtBaseState;
import com.ludot.effects.EnergisedEffect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("LudoTCaptureRule (T-9)")
class LudoTCaptureRuleTest {

    private Board board;
    private Piece makeVictim() {
        Piece v = new Piece("B1", Colour.BLUE, board);
        Cell c = new Cell(5, CellType.STANDARD, null);
        c.addPiece(v); v.setPosition(c);
        v.setDirection(Direction.COUNTER_CLOCKWISE);
        v.incrementCaptureCount();
        v.setActiveEffect(new EnergisedEffect(4));
        return v;
    }
    @Test @DisplayName("direction is cleared after capture") void directionCleared() {
        Piece v = makeVictim();
        new LudoTCaptureRule().capture(new Piece("R1", Colour.RED, board), v);
        assertNull(v.getDirection());
    }
    @Test @DisplayName("capture count is zeroed after capture") void captureCountZeroed() {
        Piece v = makeVictim();
        new LudoTCaptureRule().capture(new Piece("R1", Colour.RED, board), v);
        assertEquals(0, v.getCaptureCount());
    }
    @Test @DisplayName("victim is sent to base") void victimSentToBase() {
        Piece v = makeVictim();
        new LudoTCaptureRule().capture(new Piece("R1", Colour.RED, board), v);
        assertInstanceOf(AtBaseState.class, v.getState());
    }
}