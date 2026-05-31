package com.ludot.strategy;

import java.util.List;
import com.ludot.command.Move;
import com.ludot.engine.GameState;
import com.ludot.model.Piece;

// Aggressive Strategy: RED player prioritises capturing opponent pieces.

public class RedStrategy implements PlayerStrategy {

    public RedStrategy() {
    }

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        return null;
    }
}
