package com.ludot.command;

import com.ludot.model.Piece;

public class MoveResult {

    private final boolean success;
    private final Piece captured;           // null if no capture occurred
    private final boolean teleported;
    private final boolean grantsBonusRoll;  // T-2: true after a capture
    private final String message;

    
    public MoveResult(boolean success, Piece captured, boolean teleported,
                      boolean grantsBonusRoll, String message) {
        this.success = success;
        this.captured = captured;
        this.teleported = teleported;
        this.grantsBonusRoll = grantsBonusRoll;
        this.message = message;
    }

    
    public boolean isSuccess() { return success; }

    
    public Piece getCaptured() { return captured; }

    
    public boolean isTeleported() { return teleported; }

    
    public boolean grantsBonusRoll() { return grantsBonusRoll; }

    
    public String getMessage() { return message; }

    
    public boolean hasCaptured() { return captured != null; }
}
