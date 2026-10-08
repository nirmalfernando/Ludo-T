package com.ludot.effects;

public class EnergisedEffect implements Effect {

    private final Effect inner;
    private int roundsLeft;

    
    public EnergisedEffect(int duration) {
        this(new NoEffect(), duration);
    }

    
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

    @Override
    public int getRoundsLeft() {
        return Math.max(roundsLeft, 0);
    }
}
