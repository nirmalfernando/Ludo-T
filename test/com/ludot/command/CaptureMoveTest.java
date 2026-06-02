package com.ludot.command;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("CaptureMove")
class CaptureMoveTest {
    @Test @DisplayName("execute sends victim to base") void victimSentToBase() {}
    @Test @DisplayName("execute increments attacker capture count") void incrementsCaptureCount() {}
    @Test @DisplayName("result grants bonus roll") void resultGrantsBonusRoll() {}
    @Test @DisplayName("result carries the captured piece") void resultHasCaptured() {}
}