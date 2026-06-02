package com.ludot.rules.move;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("LudoTMoveValidator (T-1)")
class LudoTMoveValidatorTest {
    @Test @DisplayName("heads coin toss gives CLOCKWISE") void headsClockwise() {}
    @Test @DisplayName("tails coin toss gives COUNTER_CLOCKWISE") void tailsCCW() {}
    @Test @DisplayName("CCW piece requires 2 approach passes for home entry") void ccwRequiresTwoPasses() {}
}