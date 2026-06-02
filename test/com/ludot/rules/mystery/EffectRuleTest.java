package com.ludot.rules.mystery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("EffectRule (T-12/13/14)")
class EffectRuleTest {
    @Test @DisplayName("applyAlpha heads gives EnergisedEffect") void alphaHeadsEnergised() {}
    @Test @DisplayName("applyAlpha tails gives SickEffect") void alphaTailsSick() {}
    @Test @DisplayName("applyBeta sets AtBriefingState") void betaSetsBriefing() {}
    @Test @DisplayName("applyGamma flips CW to CCW") void gammaFlipsCW() {}
    @Test @DisplayName("applyGamma flips CCW to CW") void gammaFlipsCCW() {}
    @Test @DisplayName("tickAllEffects calls tickEffect on each piece") void ticksAllPieces() {}
}