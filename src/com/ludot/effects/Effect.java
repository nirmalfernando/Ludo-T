package com.ludot.effects;

// Decorator pattern — wraps step calculation so callers need no null checks or effect-type awareness.
public interface Effect {
    int modifySteps(int baseSteps);
    boolean isExpired();
    void tick();
    String getName();
}
