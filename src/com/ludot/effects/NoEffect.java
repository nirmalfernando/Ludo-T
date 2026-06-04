package com.ludot.effects;

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
