package com.ludot.rules.home;

import com.ludot.model.Direction;
import com.ludot.model.Piece;

public class BaseHomeEntryRule {

    
    public final boolean canEnterHomeStraight(Piece piece) {
        return piece.canEnterHomeStraight() && meetsAdditionalRequirements(piece);
    }

    
    protected boolean meetsAdditionalRequirements(Piece piece) {
        return true;
    }

    // rule 9: whether a piece that lands on or passes its approach cell turns into the home straight
    public boolean mayEnterHomeStraight(Piece piece, Direction direction, int approachPasses) {
        return meetsAdditionalRequirements(piece);
    }

    public int capturesNeeded(Piece piece) {
        return 0;
    }

}
