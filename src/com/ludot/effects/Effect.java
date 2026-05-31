package com.ludot.effects;

// Strategy interface representing a status effect that modifies piece movement (T-12/14).

public interface Effect {

    int modifySteps(int baseSteps);

    boolean isExpired();

    void tick();

    String getName();
}
