package com.ludot.effects;

// T-12: halves movement steps for a fixed number of rounds (Alpha mystery cell outcome).
public class SickEffect implements Effect {

    private int roundsLeft;

    public SickEffect(int duration) {
        this.roundsLeft = duration;
    }

    @Override
    public int modifySteps(int baseSteps) {
        return Math.max(1, baseSteps / 2); // minimum 1 step regardless of halving
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
        return "SICK";
    }
}
