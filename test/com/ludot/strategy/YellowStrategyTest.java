package com.ludot.strategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("YellowStrategy — progress")
class YellowStrategyTest {
    @Test @DisplayName("prefers move for piece in home straight") void prefersHomeStraight() {}
    @Test @DisplayName("falls back to first move otherwise") void fallbackToFirst() {}
}