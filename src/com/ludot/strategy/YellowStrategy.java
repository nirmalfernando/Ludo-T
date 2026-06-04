package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.List;

public class YellowStrategy extends AbstractPlayerStrategy {

    
    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        return findHomeStraight(validMoves).orElseGet(() -> firstOf(validMoves));
    }
}
