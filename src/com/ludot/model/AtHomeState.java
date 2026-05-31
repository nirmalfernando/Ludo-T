package com.ludot.model;

import com.ludot.command.Move;
import java.util.List;

// terminal state — piece has finished the game, no further movement ever
public class AtHomeState implements PieceState {

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
        return "AT_HOME";
    }
}
