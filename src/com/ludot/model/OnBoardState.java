package com.ludot.model;

import java.util.List;
import com.ludot.command.Move;

// State implementation for a piece actively moving on the main board path.

public class OnBoardState implements PieceState {

    public OnBoardState() {
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
