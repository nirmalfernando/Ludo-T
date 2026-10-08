package com.ludot.protocol;

/** Turn phase; humans act between AWAITING_ROLL and AWAITING_MOVE (and roll for the order in ROLL_OFF). */
public enum Phase {
    ROLL_OFF,       // the opening roll-off: whoever's turn it is rolls one die to decide who plays first
    AWAITING_ROLL,
    AWAITING_MOVE,
    FINISHED
}
