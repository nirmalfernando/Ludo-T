package com.ludot.strategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("RedStrategy — aggressive")
class RedStrategyTest {
    @Test @DisplayName("prefers CaptureMove over other moves") void prefersCaptureMove() {}
    @Test @DisplayName("falls back to first move when no capture") void fallbackToFirst() {}
}