package com.ludot.strategy;

import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.engine.GameState;
import com.ludot.model.Piece;
import java.util.List;

// Defensive: prefers moving an existing block, then capturing, then any other move.
public class GreenStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        for (Move move : validMoves) {
            if (move instanceof BlockMove) return move;
        }
        for (Move move : validMoves) {
            if (move instanceof CaptureMove) return move;
        }
        return validMoves.get(0);
    }
}
