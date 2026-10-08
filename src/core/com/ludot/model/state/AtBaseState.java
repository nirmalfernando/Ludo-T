package com.ludot.model.state;

import com.ludot.GameConstants;
import com.ludot.model.Piece;

import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import java.util.List;

public class AtBaseState implements PieceState {

    @Override
    public List<Move> handleRoll(Piece piece, int roll) {
        if (roll == GameConstants.LEAVE_BASE_ROLL) {
            return List.of(new MoveFromBase(piece));
        }
        return List.of();
    }

    
    public void onExitedBase(Piece piece) {
        piece.setState(new OnBoardState());
    }

    @Override
    public boolean canMove() {
        return false; // only a roll of 6 unlocks movement, handled in handleRoll
    }

    @Override
    public String getStateName() {
        return "AtBase";
    }
}
