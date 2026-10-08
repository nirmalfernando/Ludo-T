package com.ludot.rules.home;

import com.ludot.TestBoard;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BaseHomeEntryRule")
class BaseHomeEntryRuleTest {

    private Board board;
    @BeforeEach  void setUp()    { board = TestBoard.create(); }
        @Test @DisplayName("canEnterHomeStraight true at own approach cell") void trueAtApproach() {
        Piece p = new Piece("R1", Colour.RED, board);
        Cell approach = board.getApproachCell(Colour.RED);
        approach.addPiece(p); p.setPosition(approach);
        assertTrue(new BaseHomeEntryRule().canEnterHomeStraight(p));
    }
    @Test @DisplayName("canEnterHomeStraight false elsewhere") void falseElsewhere() {
        Piece p = new Piece("R1", Colour.RED, board);
        Cell other = board.getCell(10);
        other.addPiece(p); p.setPosition(other);
        assertFalse(new BaseHomeEntryRule().canEnterHomeStraight(p));
    }
}