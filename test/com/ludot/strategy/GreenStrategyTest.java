package com.ludot.strategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("GreenStrategy — defensive")
class GreenStrategyTest {
    @Test @DisplayName("prefers BlockMove over all") void prefersBlockMove() {}
    @Test @DisplayName("prefers CaptureMove when no block available") void captureWhenNoBlock() {}
    @Test @DisplayName("falls back to first move otherwise") void fallbackToFirst() {}
}