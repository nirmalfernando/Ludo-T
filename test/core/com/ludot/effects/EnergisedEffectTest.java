package com.ludot.effects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("EnergisedEffect")
class EnergisedEffectTest {
    private EnergisedEffect effect;
    @BeforeEach void setUp() { effect = new EnergisedEffect(4); }
    @Test @DisplayName("doubles base steps") void doublesBaseSteps() {
        assertEquals(6,  effect.modifySteps(3));
        assertEquals(10, effect.modifySteps(5));
    }
    @Test @DisplayName("is not expired initially") void notExpiredInitially() {
        assertFalse(effect.isExpired());
    }
    @Test @DisplayName("expires after duration ticks") void expiresAfterDuration() {
        for (int i = 0; i < 4; i++) effect.tick();
        assertTrue(effect.isExpired());
    }
    @Test @DisplayName("tick reduces remaining duration") void tickReducesDuration() {
        effect.tick();
        assertFalse(effect.isExpired());
    }
}