package com.ludot.model;

import java.util.List;
import com.ludot.command.Move;

// State implementation for a piece sitting at base, applying State pattern rules for base pieces.

public class AtBaseState implements PieceState {

    public AtBaseState() {
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
