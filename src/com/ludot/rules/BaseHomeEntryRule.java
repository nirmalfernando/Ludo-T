package com.ludot.rules;

import com.ludot.model.Piece;

// Template Method base for home-straight entry validation; subclasses may tighten entry conditions.

public class BaseHomeEntryRule {

    public BaseHomeEntryRule() {
    }

    public boolean canEnterHomeStraight(Piece piece) {
        return false;
    }

    public boolean isAtApproach(Piece piece) {
        return false;
    }
}
