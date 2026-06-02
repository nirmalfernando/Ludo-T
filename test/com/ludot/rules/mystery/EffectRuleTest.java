package com.ludot.rules.mystery;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.effects.*;
import com.ludot.model.*;
import com.ludot.model.state.AtBriefingState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("EffectRule (T-12/13/14)")
class EffectRuleTest {
    @Test @DisplayName("applyAlpha heads gives EnergisedEffect") void alphaHeadsEnergised() {
        Piece p = new Piece("R1", Colour.RED);
        new EffectRule(new FixedRandomProvider().addAlphaOutcomes(true)).applyAlpha(p);
        assertInstanceOf(EnergisedEffect.class, p.getActiveEffect());
    }
    @Test @DisplayName("applyAlpha tails gives SickEffect") void alphaTailsSick() {
        Piece p = new Piece("R1", Colour.RED);
        new EffectRule(new FixedRandomProvider().addAlphaOutcomes(false)).applyAlpha(p);
        assertInstanceOf(SickEffect.class, p.getActiveEffect());
    }
    @Test @DisplayName("applyBeta sets AtBriefingState") void betaSetsBriefing() {
        Piece p = new Piece("R1", Colour.RED);
        new EffectRule(new FixedRandomProvider()).applyBeta(p);
        assertInstanceOf(AtBriefingState.class, p.getState());
    }
    @Test @DisplayName("applyGamma flips CW to CCW") void gammaFlipsCW() {
        Piece p = new Piece("R1", Colour.RED);
        p.setDirection(Direction.CLOCKWISE);
        new EffectRule(new FixedRandomProvider()).applyGamma(p);
        assertEquals(Direction.COUNTER_CLOCKWISE, p.getDirection());
    }
    @Test @DisplayName("applyGamma flips CCW to CW") void gammaFlipsCCW() {
        Piece p = new Piece("R1", Colour.RED);
        p.setDirection(Direction.COUNTER_CLOCKWISE);
        new EffectRule(new FixedRandomProvider()).applyGamma(p);
        assertEquals(Direction.CLOCKWISE, p.getDirection());
    }
    @Test @DisplayName("tickAllEffects calls tickEffect on each piece") void ticksAllPieces() {
        Piece p1 = new Piece("R1", Colour.RED);
        Piece p2 = new Piece("R2", Colour.RED);
        p1.setActiveEffect(new EnergisedEffect(1));
        p2.setActiveEffect(new SickEffect(1));
        new EffectRule(new FixedRandomProvider()).tickAllEffects(List.of(p1, p2));
        assertInstanceOf(NoEffect.class, p1.getActiveEffect());
        assertInstanceOf(NoEffect.class, p2.getActiveEffect());
    }
}