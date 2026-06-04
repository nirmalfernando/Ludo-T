package com.ludot.effects;

// T-12: doubles movement steps for a fixed number of rounds (Alpha mystery cell outcome).
// Implements the Decorator pattern — delegates to an inner Effect before applying the 2x modifier,
// allowing effects to be stacked (e.g., Energised wrapping another effect).
public class EnergisedEffect implements Effect {

    private final Effect inner;
    private int roundsLeft;

    // Convenience constructor: wraps NoEffect as the base — used when no prior effect exists.
    public EnergisedEffect(int duration) {
        this(new NoEffect(), duration);
    }

    // Full Decorator constructor: wraps any existing Effect.
    public EnergisedEffect(Effect inner, int duration) {
        this.inner = inner;
        this.roundsLeft = duration;
    }

    @Override
    public int modifySteps(int baseSteps) {
        return inner.modifySteps(baseSteps) * 2;
    }

    @Override
    public boolean isExpired() {
        return roundsLeft <= 0;
    }

    @Override
    public void tick() {
        inner.tick();
        roundsLeft--;
    }

    @Override
    public String getName() {
        return "ENERGISED";
    }
}
