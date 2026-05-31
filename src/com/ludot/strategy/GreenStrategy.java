package com.ludot.strategy;

import java.util.List;
import com.ludot.command.Move;
import com.ludot.engine.GameState;
import com.ludot.model.Piece;

// Blocking Strategy: GREEN player prioritises forming and maintaining piece blocks.

public class GreenStrategy implements PlayerStrategy {

    public GreenStrategy() {
    }

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        return null;
    }
}
