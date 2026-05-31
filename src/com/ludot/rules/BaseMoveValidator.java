package com.ludot.rules;

import com.ludot.dice.RandomProvider;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;

// Abstract Template Method base for move validation; subclasses override direction and entry hooks.

public abstract class BaseMoveValidator {

    private final RandomProvider random;

    public BaseMoveValidator(RandomProvider random) {
        this.random = random;
    }

    public final boolean isValidMove(Piece piece, int steps) {
        return false;
    }

    public Cell getDestinationCell(Piece piece, int steps) {
        return null;
    }

    protected Direction assignDirection(Piece piece) {
        return null;
    }

    protected boolean canEnterHomeStraight(Piece piece) {
        return false;
    }
}
