package com.ludot.protocol;

/** Turn phase; humans act between AWAITING_ROLL and AWAITING_MOVE. */
public enum Phase {
    AWAITING_ROLL,
    AWAITING_MOVE,
    FINISHED
}
