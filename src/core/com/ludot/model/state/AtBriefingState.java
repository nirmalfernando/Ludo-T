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

    @Override
    public List<Move> handleRoll(Piece piece, int roll) {
        if (roll == GameConstants.BRIEFING_ESCAPE_ROLL) {
            consecutiveThrees++;
        } else {
            consecutiveThrees = 0; // streak breaks on any non-escape roll
        }

        if (consecutiveThrees == GameConstants.BRIEFING_ESCAPE_COUNT) {
            piece.resetInfo(); // T-9: clear direction, counts, effects before returning to base
            piece.sendToBase();
            return List.of();
        }

        roundsLeft--;
        if (roundsLeft <= 0) {
            piece.setState(new OnBoardState()); // briefing over — piece is free again
        }

        return List.of(); // locked regardless — no moves this turn
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
