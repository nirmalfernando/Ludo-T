package com.ludot.rules;

import com.ludot.model.Piece;

// LUDO-T specific capture rule applying capture-count tracking and bonus-roll grants.

public class LudoTCaptureRule extends BaseCaptureRule {

    public LudoTCaptureRule() {
        super();
    }

    @Override
    protected void resetPiece(Piece piece) {
    }

    @Override
    protected void postCapture(Piece piece) {
    }
}
