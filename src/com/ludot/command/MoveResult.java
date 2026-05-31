package com.ludot.command;

import com.ludot.model.Piece;

// Value object carrying the outcome of a Command execution.

public class MoveResult {

    private final boolean success;
    private final Piece captured;
    private final boolean teleported;
    private final boolean grantsBonusRoll;
    private final String message;

    public MoveResult(boolean success, Piece captured, boolean teleported,
                      boolean grantsBonusRoll, String message) {
        this.success = success;
        this.captured = captured;
        this.teleported = teleported;
        this.grantsBonusRoll = grantsBonusRoll;
        this.message = message;
    }

    public boolean hasCaptured() {
        return false;
    }

    public boolean isSuccess() {
        return success;
    }

    public Piece getCaptured() {
        return captured;
    }

    public boolean isTeleported() {
        return teleported;
    }

    public boolean isGrantsBonusRoll() {
        return grantsBonusRoll;
    }

    public String getMessage() {
        return message;
    }
}
