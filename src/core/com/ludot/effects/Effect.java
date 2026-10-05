package com.ludot.effects;

public interface Effect {
    
    int modifySteps(int baseSteps);

    
    boolean isExpired();

    
    void tick();

    
    String getName();

    /** Rounds this effect still has to run; 0 when it has none (or has expired). */
    int getRoundsLeft();
}
