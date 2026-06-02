package com.ludot.command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("MoveOnBoard")
class MoveOnBoardTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("circular move advances piece on standard path") void circularMove() {}
    @Test @DisplayName("home-straight move uses linear index") void homeStraightLinear() {}
    @Test @DisplayName("reaching last home cell sets AtHomeState") void setsAtHomeOnLast() {}
}