package com.ludot.rules;

import com.ludot.model.Piece;

public class LudoTHomeEntryRule extends BaseHomeEntryRule {

    // T-7: piece must have captured at least once before entering the home straight
    @Override
    public boolean canEnterHomeStraight(Piece piece) {
        return super.canEnterHomeStraight(piece) && piece.getCaptureCount() >= 1;
    }
}
