package com.ludot.model;

import java.util.List;
import com.ludot.command.Move;

// State implementation for a piece held at briefing (T-13 rule); tracks remaining locked rounds.

public class AtBriefingState implements PieceState {

    private int roundsLeft;
    private int consecutiveThrees;

    public AtBriefingState(int duration) {
        this.roundsLeft = duration;
        this.consecutiveThrees = 0;
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

    public int getRoundsLeft() {
        return roundsLeft;
    }
}
