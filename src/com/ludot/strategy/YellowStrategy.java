package com.ludot.strategy;

import java.util.List;
import com.ludot.command.Move;
import com.ludot.engine.GameState;
import com.ludot.model.Piece;

// Winning Strategy: YELLOW player prioritises advancing pieces towards home.

public class YellowStrategy implements PlayerStrategy {

    public YellowStrategy() {
    }

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        return null;
    }
}
