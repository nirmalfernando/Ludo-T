package com.ludot.rules.mystery;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.EnergisedEffect;
import com.ludot.effects.SickEffect;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import java.util.List;

public class EffectRule {

    private static final int EFFECT_DURATION = GameConstants.EFFECT_DURATION;
    private final RandomProvider random;

    
    public EffectRule(RandomProvider random) {
        this.random = random;
    }

    
    public void applyAlpha(Piece piece) {
        if (random.getAlphaOutcome()) {
            piece.setActiveEffect(new EnergisedEffect(EFFECT_DURATION));
        } else {
            piece.setActiveEffect(new SickEffect(EFFECT_DURATION));
        }
    }

    
    public void applyBeta(Piece piece) {
        piece.setState(new AtBriefingState(EFFECT_DURATION));
    }

    
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
