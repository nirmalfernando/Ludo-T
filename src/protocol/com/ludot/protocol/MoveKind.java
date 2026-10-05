package com.ludot.protocol;

/** What kind of move an option is. */
public enum MoveKind {
    FROM_BASE,
    MOVE,
    CAPTURE,
    BLOCK_MOVE,
    BLOCK_CAPTURE,
    ENTER_HOME_STRAIGHT,
    REACH_HOME,
    SKIP
}
