package com.ludot.strategy;

import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;

// Shared helpers for strategy subclasses — each colour overrides decideMoves() and calls these.
public abstract class AbstractPlayerStrategy implements PlayerStrategy {

    // returns the first CaptureMove in the list, or null if none exists
    protected Move findCapture(List<Move> moves) {
        for (Move m : moves) {
            if (m instanceof CaptureMove) return m;
        }
        return null;
    }

    // returns the first BlockMove in the list, or null if none exists
    protected Move findBlock(List<Move> moves) {
        for (Move m : moves) {
            if (m instanceof BlockMove) return m;
        }
        return null;
    }

    // returns the first move for a piece that is already inside the home straight
    protected Move findHomeStraight(List<Move> moves) {
        for (Move m : moves) {
            if (m instanceof MoveOnBoard mob
                    && mob.getPiece().getState() instanceof InHomeStraightState) {
                return m;
            }
        }
        return null;
    }

    // safe fallback — always returns a valid move (list is never empty; worst case is SkipMove)
    protected Move firstOf(List<Move> moves) {
        return moves.get(0);
    }
}
