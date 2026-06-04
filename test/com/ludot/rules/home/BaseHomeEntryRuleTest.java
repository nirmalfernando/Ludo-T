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
@DisplayName("BaseHomeEntryRule")
class BaseHomeEntryRuleTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); CellFactory.initializeBoard(Board.getInstance()); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("canEnterHomeStraight true at own approach cell") void trueAtApproach() {
        Piece p = new Piece("R1", Colour.RED);
        Cell approach = Board.getInstance().getApproachCell(Colour.RED);
        approach.addPiece(p); p.setPosition(approach);
        assertTrue(new BaseHomeEntryRule().canEnterHomeStraight(p));
    }
    @Test @DisplayName("canEnterHomeStraight false elsewhere") void falseElsewhere() {
        Piece p = new Piece("R1", Colour.RED);
        Cell other = Board.getInstance().getCell(10);
        other.addPiece(p); p.setPosition(other);
        assertFalse(new BaseHomeEntryRule().canEnterHomeStraight(p));
    }
}