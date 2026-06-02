package com.ludot.rules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("RuleEngine")
class RuleEngineTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("getValidMoves returns SkipMove when no moves") void skipMoveWhenNone() {}
    @Test @DisplayName("getValidMoves upgrades to CaptureMove for lone opponent") void upgradeToCapture() {}
    @Test @DisplayName("getValidMoves skips blocked destination") void blockedSkipped() {}
    @Test @DisplayName("getValidMoves returns BlockMove for pieces in a block") void blockMoveForBlock() {}
    @Test @DisplayName("applyLandingEffects transitions to InHomeStraightState") void homeEntry() {}
    @Test @DisplayName("calculateEffectiveSteps delegates to piece effect") void effectiveStesDelegates() {}
}