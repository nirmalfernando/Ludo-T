package com.ludot.rules.capture;

import com.ludot.model.Piece;

public class LudoTCaptureRule extends BaseCaptureRule {

    
    @Override
    protected void resetPiece(Piece piece) {
        piece.resetInfo();
        super.resetPiece(piece); // sends to base
    }

    
    @Override
    protected void postCapture(Piece attacker) {
        // Bonus-roll signal is conveyed through MoveResult.grantsBonusRoll in MoveExecutor.executeCapture(),
        // not here. This hook is retained for Template Method completeness.
    }
}
