package com.ludot.engine;

public enum GamePhase {
    NOT_STARTED,
    ROLL_OFF,       // the opening roll-off: the players roll one at a time to decide who starts
    AWAITING_ROLL,
    AWAITING_MOVE,
    FINISHED
}
