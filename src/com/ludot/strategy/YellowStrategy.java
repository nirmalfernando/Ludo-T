package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.engine.GameState;
import com.ludot.model.InHomeStraightState;
import com.ludot.model.Piece;
import java.util.List;

// Progress-focused: moves a piece already inside the home straight first, then any other move.
public class YellowStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        // prioritise pieces that are already inside the home straight — they are closest to winning
        for (Move move : validMoves) {
            if (move instanceof MoveOnBoard mob
                    && mob.getPiece().getState() instanceof InHomeStraightState) {
                return move;
            }
        }
        return validMoves.get(0);
    }
}
