package com.ludot.rules.capture;

import com.ludot.model.Piece;

public class BaseCaptureRule {

    
    public final boolean capture(Piece attacker, Piece victim) {
        resetPiece(victim);
        attacker.incrementCaptureCount();
        postCapture(attacker);
        return true;
    }

    
    protected void resetPiece(Piece piece) {
        piece.sendToBase();
    }

    
    protected void postCapture(Piece attacker) {}
}
