package com.ludot.model.state;

import com.ludot.GameConstants;
import com.ludot.model.Piece;

import com.ludot.command.Move;
import java.util.List;

public class AtBriefingState implements PieceState {

    private int roundsLeft;
    private int consecutiveThrees;

    
    public AtBriefingState(int duration) {
        this.roundsLeft = duration;
        this.consecutiveThrees = 0;
    }

    /** A piece in briefing cannot move, whatever is rolled. (Asking for its moves changes nothing.) */
    @Override
    public List<Move> handleRoll(Piece piece, int roll) {
        return List.of();
    }

    /**
     * T-13: called once for every roll the player makes while the piece is in briefing. Three 3s in a row teleport the
     * piece to its base (and, like any piece sent home, its information is reset: T-9).
     *
     * @return true if the piece escaped
     */
    public boolean registerRoll(Piece piece, int roll) {
        consecutiveThrees = roll == GameConstants.BRIEFING_ESCAPE_ROLL ? consecutiveThrees + 1 : 0;
        if (consecutiveThrees == GameConstants.BRIEFING_ESCAPE_COUNT) {
            piece.resetInfo();
            piece.sendToBase();
            return true;
        }
        return false;
    }

    /** T-13: called once at the end of every round; after four rounds the piece is free again. */
    public void tickRound(Piece piece) {
        roundsLeft--;
        if (roundsLeft <= 0) {
            piece.setState(new OnBoardState());
        }
    }

    @Override
    public boolean canMove() {
        return false;
    }

    public int getRoundsLeft() {
        return roundsLeft;
    }

    @Override
    public String getStateName() {
        return "AtBriefing";
    }
}
