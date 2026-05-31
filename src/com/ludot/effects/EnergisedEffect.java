package com.ludot.effects;

// Concrete Effect Strategy that doubles movement steps for a fixed duration (T-14 Beta effect).

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
        return null;
    }
}
