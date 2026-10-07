package com.ludot.rules.move;

import com.ludot.dice.RandomProvider;
import com.ludot.model.Direction;
import com.ludot.model.Piece;

public class LudoTMoveValidator extends BaseMoveValidator {

    
    public LudoTMoveValidator(RandomProvider random) {
        super(random);
    }

    
    @Override
    public boolean usesCoinToss() {
        return true;
    }

    @Override
    public Direction assignDirection(Piece piece) {
        return random.tossCoin() ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
    }

    
    @Override
    public boolean canEnterHomeStraight(Piece piece) {
        if (piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
            return piece.canEnterHomeStraight() && piece.getApproachPassCount() >= 2;
        }
        return super.canEnterHomeStraight(piece);
    }
}
