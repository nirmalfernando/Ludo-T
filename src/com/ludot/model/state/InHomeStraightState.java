package com.ludot.model.state;

import com.ludot.model.Piece;
import com.ludot.model.Cell;
import com.ludot.model.Board;

import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import java.util.List;

public class InHomeStraightState implements PieceState {

    @Override
    public List<Move> handleRoll(Piece piece, int roll) {
        List<Cell> homeStraight = Board.getInstance().getHomeStraight(piece.getColour());
        int currentIndex = homeStraight.indexOf(piece.getPosition());
        int stepsToHome = (homeStraight.size() - 1) - currentIndex;
        int steps = piece.getEffectiveSteps(roll);

        // exact roll required — overshooting is not allowed
        if (steps > stepsToHome) {
            return List.of();
        }
        return List.of(new MoveOnBoard(piece, steps));
    }

    
    public void transitionToHomeIfReached(Piece piece, int targetIndex, int lastIndex) {
        if (targetIndex == lastIndex) {
            piece.setState(new AtHomeState());
        }
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public String getStateName() {
        return "InHomeStraight";
    }
}
