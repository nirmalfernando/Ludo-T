package com.ludot.rules.mystery;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.EnergisedEffect;
import com.ludot.effects.NoEffect;
import com.ludot.effects.SickEffect;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.rules.GameRule;
import com.ludot.rules.MoveContext;
import com.ludot.rules.RuleResult;
import java.util.List;

public class EffectRule implements GameRule {

    private static final int EFFECT_DURATION   = GameConstants.EFFECT_DURATION;
    private static final int BRIEFING_DURATION = GameConstants.BRIEFING_DURATION;
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
        piece.setState(new AtBriefingState(BRIEFING_DURATION));
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

    @Override
    public boolean isApplicable(MoveContext ctx) {
        return ctx.piece() != null && !(ctx.piece().getActiveEffect() instanceof NoEffect);
    }

    @Override
    public RuleResult apply(MoveContext ctx) {
        return isApplicable(ctx) ? RuleResult.success("Active effect modifies piece movement")
                                 : RuleResult.notApplicable();
    }

    @Override
    public int getPriority() { return 10; }
}
