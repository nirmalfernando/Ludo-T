package com.ludot.rules.home;

import com.ludot.model.Piece;

public class BaseHomeEntryRule {

    
    public final boolean canEnterHomeStraight(Piece piece) {
        return piece.canEnterHomeStraight() && meetsAdditionalRequirements(piece);
    }

    
    protected boolean meetsAdditionalRequirements(Piece piece) {
        return true;
    }
}
