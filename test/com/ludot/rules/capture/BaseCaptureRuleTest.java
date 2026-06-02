package com.ludot.rules.capture;
import com.ludot.model.*;
import com.ludot.model.state.AtBaseState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BaseCaptureRule")
class BaseCaptureRuleTest {
    @Test @DisplayName("capture sends victim to base") void victimSentToBase() {
        Piece attacker = new Piece("R1", Colour.RED);
        Piece victim   = new Piece("B1", Colour.BLUE);
        Cell c = new Cell(5, CellType.STANDARD, null);
        c.addPiece(victim); victim.setPosition(c);
        new BaseCaptureRule().capture(attacker, victim);
        assertNull(victim.getPosition());
        assertInstanceOf(AtBaseState.class, victim.getState());
    }
    @Test @DisplayName("capture returns true") void captureReturnsTrue() {
        assertTrue(new BaseCaptureRule().capture(new Piece("R1", Colour.RED), new Piece("B1", Colour.BLUE)));
    }
}