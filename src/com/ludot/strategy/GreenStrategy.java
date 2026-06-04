package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.List;
import java.util.Optional;

// Blocking strategy: exit base > form/move a blockade > capture > any other move.
public class GreenStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        // 1. Always exit base on 6 — keeps pieces flowing onto the board to enable block formation
        Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);
        if (fromBase.isPresent()) {
            return fromBase.get();
        }

        // 2. Form a new blockade or advance an existing one (T-3/T-4)
        Optional<Move> block = StrategyHelper.findBlock(validMoves);
        if (block.isPresent()) {
            return block.get();
        }

        // 3. Capture if no block opportunity exists
        Optional<Move> capture = StrategyHelper.findCapture(validMoves);
        if (capture.isPresent()) {
            return capture.get();
        }

        // 4. Any remaining move
        return StrategyHelper.firstOf(validMoves);
    }
}
