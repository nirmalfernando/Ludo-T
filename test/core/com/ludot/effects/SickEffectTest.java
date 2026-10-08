package com.ludot.effects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("SickEffect")
class SickEffectTest {
    private SickEffect effect;
    @BeforeEach void setUp() { effect = new SickEffect(4); }
    @Test @DisplayName("halves base steps") void halvesBaseSteps() {
        assertEquals(2, effect.modifySteps(4));
        assertEquals(3, effect.modifySteps(6));
    }
    @Test @DisplayName("minimum steps is 1") void minimumStepsIsOne() {
        assertEquals(1, effect.modifySteps(1));
    }
    @Test @DisplayName("expires after duration ticks") void expiresAfterDuration() {
        for (int i = 0; i < 4; i++) effect.tick();
        assertTrue(effect.isExpired());
    }
}