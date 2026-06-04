package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.List;

// Defensive: prefers moving an existing block, then capturing, then any other move.
public class GreenStrategy extends AbstractPlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        Move block = findBlock(validMoves);
        if (block != null) return block;
        Move capture = findCapture(validMoves);
        return capture != null ? capture : firstOf(validMoves);
    }
}
