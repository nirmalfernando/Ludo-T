package com.ludot.rules.mystery;

import com.ludot.dice.RandomProvider;
import com.ludot.effects.EnergisedEffect;
import com.ludot.effects.SickEffect;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import java.util.List;

// T-12/13/14: applies Alpha/Beta/Gamma mystery cell effects and ticks active effects each round.
public class EffectRule {

    private static final int EFFECT_DURATION = 4;
    private final RandomProvider random;

    public EffectRule(RandomProvider random) {
        this.random = random;
    }

    // T-12: 50/50 energised or sick for EFFECT_DURATION rounds
    public void applyAlpha(Piece piece) {
        if (random.getAlphaOutcome()) {
            piece.setActiveEffect(new EnergisedEffect(EFFECT_DURATION));
        } else {
            piece.setActiveEffect(new SickEffect(EFFECT_DURATION));
        }
    }

    // T-13: lock piece in briefing state for EFFECT_DURATION rounds
    public void applyBeta(Piece piece) {
        piece.setState(new AtBriefingState(EFFECT_DURATION));
    }

    // T-14: flip direction; CW→CCW case also triggers Beta teleport (handled by RuleEngine)
    public void applyGamma(Piece piece) {
        if (piece.getDirection() == Direction.CLOCKWISE) {
            piece.setDirection(Direction.COUNTER_CLOCKWISE);
        } else {
            piece.setDirection(Direction.CLOCKWISE);
        }
    }

    public void tickAllEffects(List<Piece> pieces) {
        for (Piece piece : pieces) {
            piece.tickEffect();
        }
    }
}
