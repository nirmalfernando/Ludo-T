package com.ludot.factory;
import com.ludot.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("CellFactory")
class CellFactoryTest {
    @BeforeEach void setUp()    { Board.resetInstance(); CellFactory.initializeBoard(Board.getInstance()); }
    @AfterEach  void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("initializeBoard creates 52 standard-path cells") void standardPathSize() {
        assertEquals(52, Board.getInstance().getStandardPath().size());
    }
    @Test @DisplayName("cell 0 is YELLOW START") void cell0IsYellowStart() {
        Cell c = Board.getInstance().getCell(0);
        assertEquals(CellType.START, c.getCellType());
        assertEquals(Colour.YELLOW, c.getColour());
    }
    @Test @DisplayName("cell 50 is YELLOW APPROACH") void cell50IsYellowApproach() {
        Cell c = Board.getInstance().getCell(50);
        assertEquals(CellType.APPROACH, c.getCellType());
        assertEquals(Colour.YELLOW, c.getColour());
    }
    @Test @DisplayName("each colour has 5-cell home straight") void homeStraightSize() {
        for (Colour colour : Colour.values())
            assertEquals(5, Board.getInstance().getHomeStraight(colour).size());
    }
    @Test @DisplayName("last home-straight cell is HOME type") void lastCellIsHome() {
        for (Colour colour : Colour.values()) {
            var hs = Board.getInstance().getHomeStraight(colour);
            assertEquals(CellType.HOME, hs.get(hs.size()-1).getCellType());
        }
    }
}