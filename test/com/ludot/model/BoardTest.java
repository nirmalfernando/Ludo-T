package com.ludot.model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Board (Singleton)")
class BoardTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("getInstance returns the same object") void singletonIdentity() {}
    @Test @DisplayName("getCell throws on out-of-range id") void getCellOutOfRange() {}
    @Test @DisplayName("getStartCell correct for each colour") void startCells() {}
    @Test @DisplayName("getApproachCell correct for each colour") void approachCells() {}
    @Test @DisplayName("getStandardPath returns unmodifiable list of 52") void standardPathSize() {}
}