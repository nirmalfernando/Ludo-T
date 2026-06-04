package com.ludot.model.state;
import com.ludot.model.Piece;

import com.ludot.command.Move;
import java.util.List;

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
