package com.ludot.rules;

import com.ludot.model.Piece;

// Template Method base for capture logic; subclasses override reset and post-capture hooks.

public class BaseCaptureRule {

    public BaseCaptureRule() {
    }

    public final boolean capture(Piece attacker, Piece victim) {
        return false;
    }

    protected void resetPiece(Piece piece) {
    }

    protected void postCapture(Piece piece) {
    }
}
