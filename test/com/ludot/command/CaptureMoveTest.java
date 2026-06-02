package com.ludot.command;
import com.ludot.model.*;
import com.ludot.model.state.AtBaseState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("CaptureMove")
class CaptureMoveTest {
    private Piece attacker, victim;
    private Cell attackerCell, targetCell;
    @BeforeEach void setUp() {
        attacker    = new Piece("RED1",  Colour.RED);
        victim      = new Piece("BLUE1", Colour.BLUE);
        attackerCell = new Cell(4, CellType.STANDARD, null);
        targetCell   = new Cell(5, CellType.STANDARD, null);
        attackerCell.addPiece(attacker); attacker.setPosition(attackerCell);
        targetCell.addPiece(victim);     victim.setPosition(targetCell);
    }
    @Test @DisplayName("execute sends victim to base") void victimSentToBase() {
        new CaptureMove(attacker, victim, targetCell, 1).execute();
        assertNull(victim.getPosition());
        assertInstanceOf(AtBaseState.class, victim.getState());
    }
    @Test @DisplayName("execute increments attacker capture count") void incrementsCaptureCount() {
        new CaptureMove(attacker, victim, targetCell, 1).execute();
        assertEquals(1, attacker.getCaptureCount());
    }
    @Test @DisplayName("result grants bonus roll") void resultGrantsBonusRoll() {
        assertTrue(new CaptureMove(attacker, victim, targetCell, 1).execute().grantsBonusRoll());
    }
    @Test @DisplayName("result carries the captured piece") void resultHasCaptured() {
        MoveResult r = new CaptureMove(attacker, victim, targetCell, 1).execute();
        assertTrue(r.hasCaptured());
        assertEquals(victim, r.getCaptured());
    }
}