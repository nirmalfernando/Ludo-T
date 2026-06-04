package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.List;

// Unpredictable: cycles through valid moves using the game-wide turn counter.
// Fully stateless — no mutable fields; the cycle index is derived from GameState.getTurnNumber()
// so the selection is a pure function of game state, not accumulated strategy-side state.
public class BlueStrategy extends AbstractPlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        return validMoves.get(gameState.getTurnNumber() % validMoves.size());
    }
}
