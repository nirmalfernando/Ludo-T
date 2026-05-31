package com.ludot.rules;

import java.util.List;
import com.ludot.dice.RandomProvider;
import com.ludot.model.Piece;

// Encapsulates Alpha/Beta/Gamma effect application and per-round effect ticking (T-12/13/14).

public class EffectRule {

    private final RandomProvider random;

    public EffectRule(RandomProvider random) {
        this.random = random;
    }

    public void applyAlpha(Piece piece) {
    }

    public void applyBeta(Piece piece) {
    }

    public void applyGamma(Piece piece) {
    }

    public void tickAllEffects(List<Piece> pieces) {
    }
}
