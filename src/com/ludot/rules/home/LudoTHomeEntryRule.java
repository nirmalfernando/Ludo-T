package com.ludot.rules.home;

import com.ludot.model.Piece;

// T-7: adds a capture gate — piece must have captured at least once before entering home straight.
public class LudoTHomeEntryRule extends BaseHomeEntryRule {

    @Override
    protected boolean meetsAdditionalRequirements(Piece piece) {
        return piece.getCaptureCount() >= 1;
    }
}
