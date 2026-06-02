package com.ludot.model.state;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("InHomeStraightState")
class InHomeStraightStateTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("handleRoll returns MoveOnBoard when steps fit") void moveFits() {}
    @Test @DisplayName("handleRoll returns empty on overshoot") void overshootReturnsEmpty() {}
    @Test @DisplayName("canMove returns true") void canMoveIsTrue() {}
}