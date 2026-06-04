package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.List;

// Aggressive: always capture if possible, otherwise take the first available move.
public class RedStrategy extends AbstractPlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        Move capture = findCapture(validMoves);
        return capture != null ? capture : firstOf(validMoves);
    }
}
