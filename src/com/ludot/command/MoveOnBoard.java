package com.ludot.command;

import com.ludot.model.Piece;

// Command that advances an on-board piece by a given number of steps.

public class MoveOnBoard implements Move {

    private final Piece piece;
    private final int steps;

    public MoveOnBoard(Piece piece, int steps) {
        this.piece = piece;
        this.steps = steps;
    }

    @Override
    public MoveResult execute() {
        return null;
    }

    @Override
    public String describe() {
        return null;
    }
}
