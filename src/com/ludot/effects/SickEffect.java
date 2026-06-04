package com.ludot.effects;

// T-12: halves movement steps for a fixed number of rounds (Alpha mystery cell outcome).
// Implements the Decorator pattern — delegates to an inner Effect before applying the /2 modifier,
// allowing effects to be stacked.
public class SickEffect implements Effect {

    private final Effect inner;
    private int roundsLeft;

    // Convenience constructor: wraps NoEffect as the base — used when no prior effect exists.
    public SickEffect(int duration) {
        this(new NoEffect(), duration);
    }

    // Full Decorator constructor: wraps any existing Effect.
    public SickEffect(Effect inner, int duration) {
        this.inner = inner;
        this.roundsLeft = duration;
    }

    @Override
    public int modifySteps(int baseSteps) {
        return Math.max(1, inner.modifySteps(baseSteps) / 2);
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
        return "SICK";
    }
}
