package com.ludot.effects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("NoEffect (Null Object)")
class NoEffectTest {
    private final NoEffect effect = new NoEffect();
    @Test @DisplayName("passes steps through unchanged") void passesStepsThrough() {}
    @Test @DisplayName("is always expired") void isAlwaysExpired() {}
    @Test @DisplayName("tick is a no-op") void tickIsNoOp() {}
}