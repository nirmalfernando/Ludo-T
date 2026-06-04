package com.ludot.model;
import com.ludot.factory.CellFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Board (Singleton)")
class BoardTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); CellFactory.initializeBoard(Board.getInstance()); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("getInstance returns the same object") void singletonIdentity() {
        assertSame(Board.getInstance(), Board.getInstance());
    }
    @Test @DisplayName("getCell throws on out-of-range id") void getCellOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Board.getInstance().getCell(-1));
        assertThrows(IllegalArgumentException.class, () -> Board.getInstance().getCell(52));
    }
    @Test @DisplayName("getStartCell correct for each colour") void startCells() {
        assertEquals(0,  Board.getInstance().getStartCell(Colour.YELLOW).getId());
        assertEquals(13, Board.getInstance().getStartCell(Colour.BLUE).getId());
        assertEquals(26, Board.getInstance().getStartCell(Colour.RED).getId());
        assertEquals(39, Board.getInstance().getStartCell(Colour.GREEN).getId());
    }
    @Test @DisplayName("getApproachCell correct for each colour") void approachCells() {
        assertEquals(50, Board.getInstance().getApproachCell(Colour.YELLOW).getId());
        assertEquals(11, Board.getInstance().getApproachCell(Colour.BLUE).getId());
        assertEquals(24, Board.getInstance().getApproachCell(Colour.RED).getId());
        assertEquals(37, Board.getInstance().getApproachCell(Colour.GREEN).getId());
    }
    @Test @DisplayName("getStandardPath returns unmodifiable list of 52") void standardPathSize() {
        assertEquals(52, Board.getInstance().getStandardPath().size());
        assertThrows(UnsupportedOperationException.class,
            () -> Board.getInstance().getStandardPath().add(null));
    }
}