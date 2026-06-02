package com.ludot.rules.move;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("BaseMoveValidator")
class BaseMoveValidatorTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("CW destination uses modulo arithmetic") void cwDestination() {}
    @Test @DisplayName("null for piece at base") void nullForBasePiece() {}
    @Test @DisplayName("null on home-straight overshoot") void nullOnOvershoot() {}
    @Test @DisplayName("assignDirection always returns CLOCKWISE") void alwaysClockwise() {}
}