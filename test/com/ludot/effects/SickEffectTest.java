package com.ludot.effects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("SickEffect")
class SickEffectTest {
    private SickEffect effect;
    @BeforeEach void setUp() { effect = new SickEffect(4); }
    @Test @DisplayName("halves base steps") void halvesBaseSteps() {}
    @Test @DisplayName("minimum steps is 1") void minimumStepsIsOne() {}
    @Test @DisplayName("expires after duration ticks") void expiresAfterDuration() {}
}