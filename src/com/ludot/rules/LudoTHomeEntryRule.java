package com.ludot.rules;

import com.ludot.model.Piece;

// LUDO-T specific home-entry rule enforcing the approach-pass-count requirement.

public class LudoTHomeEntryRule extends BaseHomeEntryRule {

    public LudoTHomeEntryRule() {
        super();
    }

    @Override
    public boolean canEnterHomeStraight(Piece piece) {
        return false;
    }
}
