package com.ludot.model;

import com.ludot.command.Move;
import java.util.List;

// T-13: piece is locked after landing on Beta mystery cell; triple-3 sends it back to base early
public class AtBriefingState implements PieceState {

    private int roundsLeft;
    private int consecutiveThrees;

    public AtBriefingState(int duration) {
        this.roundsLeft = duration;
        this.consecutiveThrees = 0;
    }

    @Override
    public List<Move> handleRoll(Piece piece, int roll) {
        if (roll == 3) {
            consecutiveThrees++;
        } else {
            consecutiveThrees = 0; // streak breaks on any non-3 roll
        }

        if (consecutiveThrees == 3) {
            piece.sendToBase(); // sendToBase transitions state to AtBaseState internally
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

    @Override
    public String getStateName() {
        return "AT_BRIEFING";
    }

    public int getRoundsLeft() {
        return roundsLeft;
    }
}
