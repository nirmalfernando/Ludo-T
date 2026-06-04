package com.ludot.rules.home;

import com.ludot.model.Piece;

public class LudoTHomeEntryRule extends BaseHomeEntryRule {

    
    @Override
    protected boolean meetsAdditionalRequirements(Piece piece) {
        return piece.getCaptureCount() >= 1;
    }
}
