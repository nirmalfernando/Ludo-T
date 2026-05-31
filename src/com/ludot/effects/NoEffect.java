package com.ludot.effects;

// Null Object implementation of Effect; represents the absence of any active status effect.

public class NoEffect implements Effect {

    public NoEffect() {
    }

    @Override
    public int modifySteps(int baseSteps) {
        return baseSteps;
    }

    @Override
    public boolean isExpired() {
        return true;
    }

    @Override
    public void tick() {
    }

    @Override
    public String getName() {
        return null;
    }
}
