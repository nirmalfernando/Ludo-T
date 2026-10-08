package com.ludot.rules.move;

import com.ludot.dice.RandomProvider;
import com.ludot.model.Direction;
import com.ludot.model.Piece;

public abstract class BaseMoveValidator {

    protected final RandomProvider random;

    
    public BaseMoveValidator(RandomProvider random) {
        this.random = random;
    }

    
    public boolean usesCoinToss() {
        return false;
    }

    public Direction assignDirection(Piece piece) {
        return Direction.CLOCKWISE;
    }

}
