package com.ludot.rules;

import com.ludot.dice.RandomProvider;
import com.ludot.model.Direction;
import com.ludot.model.Piece;

// LUDO-T specific move validator applying coin-toss direction assignment and approach-count rules.

public class LudoTMoveValidator extends BaseMoveValidator {

    public LudoTMoveValidator(RandomProvider random) {
        super(random);
    }

    @Override
    protected Direction assignDirection(Piece piece) {
        return null;
    }

    @Override
    protected boolean canEnterHomeStraight(Piece piece) {
        return false;
    }
}
