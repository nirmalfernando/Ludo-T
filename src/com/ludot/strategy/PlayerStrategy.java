package com.ludot.strategy;

import java.util.List;
import com.ludot.command.Move;
import com.ludot.model.GameState;
import com.ludot.model.Piece;

// Strategy interface for player AI decision-making; each colour implements its own heuristic.

public interface PlayerStrategy {

    Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves);
}
