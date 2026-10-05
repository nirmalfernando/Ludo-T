package com.ludot.strategy;

import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.List;
import java.util.Optional;

// Cyclic strategy: rotate through pieces B1→B2→B3→B4 each turn.
// CCW pieces seek the mystery cell; CW pieces avoid it.
// When pieces list is empty (e.g. tests), falls back to cycling by move index.
public class BlueStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        // Fallback when no piece context available (stateless cycling by move index)
        if (pieces.isEmpty() || gameState == null) {
            int idx = gameState != null ? gameState.getTurnNumber() % validMoves.size() : 0;
            return validMoves.get(idx);
        }

        int pieceCount = pieces.size();
        int startIdx   = gameState.getTurnNumber() % pieceCount;

        // Cycle through pieces starting at the designated one for this turn (B1→B2→B3→B4)
        for (int i = 0; i < pieceCount; i++) {
            Piece target = pieces.get((startIdx + i) % pieceCount);
            Optional<Move> move = findMoveForPiece(validMoves, target);
            if (move.isPresent()) {
                // Apply mystery cell preference if the piece has a direction assigned
                if (target.getDirection() != null) {
                    Optional<Move> preferred = applyMysteryPreference(validMoves, target, move.get(), gameState);
                    return preferred.orElse(move.get());
                }
                return move.get();
            }
        }

        // No piece-specific move found — fall back to cycling by move index
        return validMoves.get(gameState.getTurnNumber() % validMoves.size());
    }

    // Finds the first valid move in the list that belongs to the target piece.
    private Optional<Move> findMoveForPiece(List<Move> moves, Piece target) {
        return moves.stream()
                .filter(m -> target.equals(StrategyHelper.getPieceFromMove(m)))
                .findFirst();
    }

    // CCW pieces seek the mystery cell (prefer landing on it);
    // CW pieces avoid it (prefer an alternative if the default move would land on it).
    private Optional<Move> applyMysteryPreference(List<Move> validMoves, Piece piece,
                                                   Move defaultMove, GameState state) {
        Cell mysteryCell = findMysteryCell(state);
        if (mysteryCell == null) return Optional.empty();

        if (piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
            // Seek: prefer a move for this piece that lands exactly on the mystery cell
            return validMoves.stream()
                    .filter(m -> piece.equals(StrategyHelper.getPieceFromMove(m)))
                    .filter(m -> {
                        Cell dest = StrategyHelper.computeDest(piece, getSteps(m));
                        return dest != null && dest.getId() == mysteryCell.getId();
                    })
                    .findFirst();
        } else {
            // Avoid: if the default move would land on the mystery cell, prefer an alternative
            Cell defaultDest = StrategyHelper.computeDest(piece, getSteps(defaultMove));
            if (defaultDest != null && defaultDest.getId() == mysteryCell.getId()) {
                return validMoves.stream()
                        .filter(m -> piece.equals(StrategyHelper.getPieceFromMove(m)))
                        .filter(m -> {
                            Cell dest = StrategyHelper.computeDest(piece, getSteps(m));
                            return dest == null || dest.getId() != mysteryCell.getId();
                        })
                        .findFirst();
            }
            return Optional.empty();
        }
    }

    private Cell findMysteryCell(GameState state) {
        for (Cell cell : state.getBoard().getStandardPath()) {
            if (cell.isMysteryCell()) {
                return cell;
            }
        }
        return null;
    }

    private int getSteps(Move move) {
        if (move instanceof MoveOnBoard mob) return mob.getSteps();
        if (move instanceof CaptureMove cm)  return cm.getSteps();
        if (move instanceof MoveFromBase)    return 0;
        return 0;
    }
}
