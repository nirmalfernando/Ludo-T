package com.ludot.effects;

// Concrete Effect Strategy that halves movement steps for a fixed duration (T-14 Gamma effect).

public class SickEffect implements Effect {

    private int roundsLeft;

    public SickEffect(int duration) {
        this.roundsLeft = duration;
    }

    @Override
    public int modifySteps(int baseSteps) {
        return Math.max(1, baseSteps / 2);
    }

    @Override
    public boolean isExpired() {
        return roundsLeft <= 0;
    }

    @Override
    public void tick() {
        roundsLeft--;
    }

    @Override
    public String getName() {
        return null;
    }
}
