package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.engine.GameState;
import com.ludot.model.Piece;
import java.util.List;

// Unpredictable: rotates through valid moves via a cycling index rather than any fixed heuristic.
public class BlueStrategy implements PlayerStrategy {

    private int cycleIndex = 0;

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        Move chosen = validMoves.get(cycleIndex % validMoves.size());
        cycleIndex++;
        return chosen;
    }
}
