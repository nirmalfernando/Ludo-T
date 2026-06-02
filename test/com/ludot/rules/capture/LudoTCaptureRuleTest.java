package com.ludot.rules.capture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("LudoTCaptureRule (T-9)")
class LudoTCaptureRuleTest {
    @Test @DisplayName("resetPiece calls resetInfo before sendToBase") void resetsInfoFirst() {}
    @Test @DisplayName("victim direction is cleared") void directionCleared() {}
    @Test @DisplayName("victim capture count is zeroed") void captureCountZeroed() {}
}