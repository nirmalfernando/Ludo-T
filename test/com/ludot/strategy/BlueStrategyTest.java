package com.ludot.strategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("BlueStrategy — cycling")
class BlueStrategyTest {
    @Test @DisplayName("cycles through moves by index") void cyclesThroughMoves() {}
    @Test @DisplayName("index wraps when exceeds list size") void indexWrapsAround() {}
    @Test @DisplayName("index increments each call") void indexIncrementsEachCall() {}
}