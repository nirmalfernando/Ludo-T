package com.ludot.factory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
import com.ludot.model.CellType;
@DisplayName("CellFactory")
class CellFactoryTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("initializeBoard creates 52 standard-path cells") void standardPathSize() {}
    @Test @DisplayName("cell 0 is YELLOW START") void cell0IsYellowStart() {}
    @Test @DisplayName("cell 50 is YELLOW APPROACH") void cell50IsYellowApproach() {}
    @Test @DisplayName("each colour has 5-cell home straight") void homeStraightSize() {}
    @Test @DisplayName("last home-straight cell is HOME type") void lastCellIsHome() {}
}