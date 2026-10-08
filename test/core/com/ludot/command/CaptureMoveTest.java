package com.ludot.command;

import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("CaptureMove")
class CaptureMoveTest {

    private Board board;
    private Piece attacker, victim;
    private Cell attackerCell, targetCell;
    @BeforeEach void setUp() {
        attacker    = new Piece("RED1",  Colour.RED, board);
        victim      = new Piece("BLUE1", Colour.BLUE, board);
        attackerCell = new Cell(4, CellType.STANDARD, null);
        targetCell   = new Cell(5, CellType.STANDARD, null);
        attackerCell.addPiece(attacker); attacker.setPosition(attackerCell);
        targetCell.addPiece(victim);     victim.setPosition(targetCell);
    }
    @Test @DisplayName("execute moves attacker to target cell") void attackerMovesToTarget() {
        new CaptureMove(attacker, victim, targetCell, 1).execute();
        assertEquals(targetCell, attacker.getPosition());
        assertFalse(attackerCell.getPieces().contains(attacker));
    }
    @Test @DisplayName("victim reset is delegated — victim still on cell until captureRule.capture() is called")
    void victimResetDelegatedToCaptureRule() {
        // CaptureMove only moves the attacker; victim reset is MoveExecutor's responsibility
        new CaptureMove(attacker, victim, targetCell, 1).execute();
        // victim is still at targetCell until captureRule.capture() is explicitly called
        assertNotNull(victim.getPosition());
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