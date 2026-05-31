package com.ludot.strategy;

import java.util.List;
import com.ludot.command.Move;
import com.ludot.engine.GameState;
import com.ludot.model.Piece;

// Random/Mystery-seeking Strategy: BLUE player pursues mystery cells using a cycling index.

public class BlueStrategy implements PlayerStrategy {

    private int cycleIndex;

    public BlueStrategy() {
        this.cycleIndex = 0;
    }

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        return null;
    }
}
