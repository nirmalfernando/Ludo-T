package com.ludot.rules;

import com.ludot.model.Piece;

// Template Method — defines capture sequence; subclasses override victim reset and post-capture hooks.
public class BaseCaptureRule {

    // final: always runs resetPiece then postCapture in this order
    public final boolean capture(Piece attacker, Piece victim) {
        resetPiece(victim);
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
