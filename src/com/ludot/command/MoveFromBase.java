package com.ludot.command;

import com.ludot.model.Piece;

// Command that moves a piece from base onto the board's start cell.

public class MoveFromBase implements Move {

    private final Piece piece;

    public MoveFromBase(Piece piece) {
        this.piece = piece;
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
