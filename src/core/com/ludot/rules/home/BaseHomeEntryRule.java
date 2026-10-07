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

    /**
     * Rule 9: whether a piece that lands on or passes its approach cell turns into the home straight.
     * Classic always lets it; {@code approachPasses} is how many times the piece has reached the approach cell.
     */
    public boolean mayEnterHomeStraight(Piece piece, Direction direction, int approachPasses) {
        return meetsAdditionalRequirements(piece);
    }

    /** How many more captures the piece needs before it may enter the home straight (0 in Classic). */
    public int capturesNeeded(Piece piece) {
        return 0;
    }

}
