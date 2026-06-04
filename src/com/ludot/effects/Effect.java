package com.ludot.effects;

public interface Effect {
    
    int modifySteps(int baseSteps);

    
    boolean isExpired();

    
    void tick();

    
    String getName();
}
