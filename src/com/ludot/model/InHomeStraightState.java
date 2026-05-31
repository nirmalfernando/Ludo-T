package com.ludot.model;

import java.util.List;
import com.ludot.command.Move;

// State implementation for a piece travelling down the colour-specific home straight.

public class InHomeStraightState implements PieceState {

    public InHomeStraightState() {
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
