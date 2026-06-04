package com.ludot.model.state;
import com.ludot.model.Piece;

import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import java.util.List;

public class OnBoardState implements PieceState {

    @Override
    public List<Move> handleRoll(Piece piece, int roll) {
        int steps = piece.getEffectiveSteps(roll);
        return List.of(new MoveOnBoard(piece, steps));
    }

    // State-owned transition: called by RuleEngine after physically placing the piece in the home straight.
    public void transitionToHomeStraight(Piece piece) {
        piece.setState(new InHomeStraightState());
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public String getStateName() {
        return "ON_BOARD";
    }
}
