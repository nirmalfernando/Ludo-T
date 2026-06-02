package com.ludot.effects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("EnergisedEffect")
class EnergisedEffectTest {
    private EnergisedEffect effect;
    @BeforeEach void setUp() { effect = new EnergisedEffect(4); }
    @Test @DisplayName("doubles base steps") void doublesBaseSteps() {}
    @Test @DisplayName("is not expired initially") void notExpiredInitially() {}
    @Test @DisplayName("expires after duration ticks") void expiresAfterDuration() {}
    @Test @DisplayName("tick reduces remaining duration") void tickReducesDuration() {}
}