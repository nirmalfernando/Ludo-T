package com.ludot.effects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("NoEffect (Null Object)")
class NoEffectTest {
    private final NoEffect effect = new NoEffect();
    @Test @DisplayName("passes steps through unchanged") void passesStepsThrough() {
        assertEquals(5, effect.modifySteps(5));
        assertEquals(1, effect.modifySteps(1));
    }
    @Test @DisplayName("is always expired") void isAlwaysExpired() {
        assertTrue(effect.isExpired());
    }
    @Test @DisplayName("tick is a no-op") void tickIsNoOp() {
        effect.tick();
        assertTrue(effect.isExpired());
    }
}