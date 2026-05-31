package com.ludot.effects;

// T-12: doubles movement steps for a fixed number of rounds (Alpha mystery cell outcome).
public class EnergisedEffect implements Effect {

    private int roundsLeft;

    public EnergisedEffect(int duration) {
        this.roundsLeft = duration;
    }

    @Override
    public int modifySteps(int baseSteps) {
        return baseSteps * 2;
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
        return "ENERGISED";
    }
}
