package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.engine.GameState;
import com.ludot.model.Piece;
import java.util.List;

// Progress-focused: moves a piece already inside the home straight first, then any other move.
public class YellowStrategy extends AbstractPlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        Move hs = findHomeStraight(validMoves);
        return hs != null ? hs : firstOf(validMoves);
    }
}
