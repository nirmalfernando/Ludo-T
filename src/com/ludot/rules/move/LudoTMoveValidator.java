package com.ludot.rules.move;

import com.ludot.dice.RandomProvider;
import com.ludot.model.Direction;
import com.ludot.model.Piece;

public class LudoTMoveValidator extends BaseMoveValidator {

    public LudoTMoveValidator(RandomProvider random) {
        super(random);
    }

    // T-1: direction assigned by coin toss when piece leaves base
    @Override
    public Direction assignDirection(Piece piece) {
        return random.tossCoin() ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
    }

    // T-1: CCW pieces must have passed the approach cell at least twice
    @Override
    public boolean canEnterHomeStraight(Piece piece) {
        if (piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
            return piece.canEnterHomeStraight() && piece.getApproachPassCount() >= 2;
        }
        return super.canEnterHomeStraight(piece);
    }
}
