package com.ludot.rules;

import com.ludot.model.Piece;

public class LudoTCaptureRule extends BaseCaptureRule {

    // T-9: clear ALL piece info before sending to base (direction, counts, effects, passes)
    @Override
    protected void resetPiece(Piece piece) {
        piece.resetInfo();
        super.resetPiece(piece); // sends to base
    }

    // T-2: bonus roll after capture — communicated via CaptureMove returning grantsBonusRoll=true
    @Override
    protected void postCapture(Piece attacker) {}
}
