package com.ludot.rules;

import com.ludot.model.Piece;

public class BaseHomeEntryRule {

    public boolean isAtApproach(Piece piece) {
        return piece.canEnterHomeStraight();
    }

    // base game: being at the approach cell is enough
    public boolean canEnterHomeStraight(Piece piece) {
        return isAtApproach(piece);
    }
}
