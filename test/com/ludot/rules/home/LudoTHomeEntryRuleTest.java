package com.ludot.rules.home;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("LudoTHomeEntryRule (T-7)")
class LudoTHomeEntryRuleTest {
    private Piece piece;
    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        piece = new Piece("R1", Colour.RED);
        Cell approach = Board.getInstance().getApproachCell(Colour.RED);
        approach.addPiece(piece); piece.setPosition(approach);
    }
    @AfterEach void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("false with zero captures even at approach cell") void falseWithNoCaptures() {
        assertFalse(new LudoTHomeEntryRule().canEnterHomeStraight(piece));
    }
    @Test @DisplayName("true with at least one capture at approach cell") void requiresCapture() {
        piece.incrementCaptureCount();
        assertTrue(new LudoTHomeEntryRule().canEnterHomeStraight(piece));
    }
    @Test @DisplayName("false when not at approach cell even with capture") void falseNotAtApproach() {
        piece.incrementCaptureCount();
        Cell other = Board.getInstance().getCell(10);
        piece.getPosition().removePiece(piece);
        other.addPiece(piece); piece.setPosition(other);
        assertFalse(new LudoTHomeEntryRule().canEnterHomeStraight(piece));
    }
}