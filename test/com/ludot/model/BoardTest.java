package com.ludot.model;

import com.ludot.TestBoard;
import com.ludot.factory.CellFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Board (Singleton)")
class BoardTest {

    private Board board;
    @BeforeEach  void setUp()    { board = TestBoard.create(); }
        @Test @DisplayName("getInstance returns the same object") void singletonIdentity() {
        assertSame(board, board);
    }
    @Test @DisplayName("getCell throws on out-of-range id") void getCellOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> board.getCell(-1));
        assertThrows(IllegalArgumentException.class, () -> board.getCell(52));
    }
    @Test @DisplayName("getStartCell correct for each colour") void startCells() {
        assertEquals(0,  board.getStartCell(Colour.YELLOW).getId());
        assertEquals(13, board.getStartCell(Colour.BLUE).getId());
        assertEquals(26, board.getStartCell(Colour.RED).getId());
        assertEquals(39, board.getStartCell(Colour.GREEN).getId());
    }
    @Test @DisplayName("getApproachCell correct for each colour") void approachCells() {
        assertEquals(50, board.getApproachCell(Colour.YELLOW).getId());
        assertEquals(11, board.getApproachCell(Colour.BLUE).getId());
        assertEquals(24, board.getApproachCell(Colour.RED).getId());
        assertEquals(37, board.getApproachCell(Colour.GREEN).getId());
    }
    @Test @DisplayName("getStandardPath returns unmodifiable list of 52") void standardPathSize() {
        assertEquals(52, board.getStandardPath().size());
        assertThrows(UnsupportedOperationException.class,
            () -> board.getStandardPath().add(null));
    }
}