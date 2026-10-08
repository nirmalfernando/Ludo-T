package com.ludot.protocol;

public enum Phase {
    ROLL_OFF,       // the opening roll-off: whoever's turn it is rolls one die to decide who plays first
    AWAITING_ROLL,
    AWAITING_MOVE,
    FINISHED
}
