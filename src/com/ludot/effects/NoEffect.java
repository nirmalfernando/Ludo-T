package com.ludot.effects;

// Null Object — default state when no effect is active; steps pass through unchanged.
public class NoEffect implements Effect {

    @Override
    public int modifySteps(int baseSteps) {
        return baseSteps;
    }

    @Override
    public boolean isExpired() {
        return true;
    }

    @Override
    public void tick() {}

    @Override
    public String getName() {
        return "NONE";
    }
}
