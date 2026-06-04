package com.ludot.strategy;

import com.ludot.GameConstants;
import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;
import java.util.Optional;

// Shared static helpers for all PlayerStrategy implementations.
// Strategies implement PlayerStrategy directly and call these as needed — no shared base class.
final class StrategyHelper {

    private StrategyHelper() {
    }

    static Optional<Move> findCapture(List<Move> moves) {
        return moves.stream().filter(m -> m instanceof CaptureMove).findFirst();
    }

    static Optional<Move> findBlock(List<Move> moves) {
        return moves.stream().filter(m -> m instanceof BlockMove).findFirst();
    }

    static Optional<Move> findHomeStraight(List<Move> moves) {
        return moves.stream()
                .filter(m -> m instanceof MoveOnBoard mob
                        && mob.getPiece().getState() instanceof InHomeStraightState)
                .findFirst();
    }

    static Optional<Move> findMoveFromBase(List<Move> moves) {
        return moves.stream().filter(m -> m instanceof MoveFromBase).findFirst();
    }

    static Move firstOf(List<Move> moves) {
        return moves.get(0);
    }

    // Returns the primary piece associated with a move, or null for SkipMove.
    static Piece getPieceFromMove(Move move) {
        if (move instanceof MoveFromBase mfb) return mfb.getPiece();
        if (move instanceof MoveOnBoard mob)  return mob.getPiece();
        if (move instanceof CaptureMove cm)   return cm.getAttacker();
        if (move instanceof BlockMove bm)     return bm.getBlock().get(0);
        return null;
    }

    // Computes the destination cell for a piece moving the given steps in its current direction,
    // without actually moving the piece. Returns null if position or direction is unset.
    static Cell computeDest(Piece piece, int steps) {
        if (piece.getPosition() == null || piece.getDirection() == null) {
            return null;
        }
        int id = piece.getPosition().getId();
        for (int i = 0; i < steps; i++) {
            if (piece.getDirection() == Direction.CLOCKWISE) {
                id = (id + 1) % GameConstants.BOARD_SIZE;
            } else {
                id = ((id - 1) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
            }
        }
        return Board.getInstance().getCell(id);
    }

    // Minimum circular distance between two cell ids on the 52-cell board.
    static int circularDist(int a, int b) {
        int diff = Math.abs(a - b);
        return Math.min(diff, GameConstants.BOARD_SIZE - diff);
    }
}
