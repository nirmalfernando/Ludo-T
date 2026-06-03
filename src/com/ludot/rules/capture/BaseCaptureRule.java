package com.ludot.rules.capture;

import com.ludot.model.Piece;

// Template Method — defines capture sequence; subclasses override victim reset and post-capture hooks.
public class BaseCaptureRule {

    // final: always runs resetPiece, increments attacker count, then postCapture
    public final boolean capture(Piece attacker, Piece victim) {
        resetPiece(victim);
        attacker.incrementCaptureCount();
        postCapture(attacker);
        return true;
    }

    // base game: just send victim back to base
    protected void resetPiece(Piece piece) {
        piece.sendToBase();
    }

    // base game: no extra reward for the attacker
    protected void postCapture(Piece attacker) {}
}
