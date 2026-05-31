package com.ludot.model;

import java.util.List;
import com.ludot.command.Move;

// Terminal State implementation for a piece that has reached home; movement is never allowed.

public class AtHomeState implements PieceState {

    public AtHomeState() {
    }

    @Override
    public List<Move> handleRoll(Piece piece, int roll) {
        return List.of();
    }

    @Override
    public boolean canMove() {
        return false;
    }

    @Override
    public String getStateName() {
        return null;
    }
}
