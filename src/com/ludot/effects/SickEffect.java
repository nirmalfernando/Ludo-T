package com.ludot.effects;

public class SickEffect implements Effect {

    private final Effect inner;
    private int roundsLeft;

    
    public SickEffect(int duration) {
        this(new NoEffect(), duration);
    }

    
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
