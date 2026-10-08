package com.ludot.strategy;

import com.ludot.GameConstants;
import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;
import java.util.Optional;

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
        return piece.getBoard().getCell(id);
    }

    // Minimum circular distance between two cell ids on the 52-cell board.
    static int circularDist(int a, int b) {
        int diff = Math.abs(a - b);
        return Math.min(diff, GameConstants.BOARD_SIZE - diff);
    }

    // The cell a move ends on, as the rule engine planned it (home entry and blockades included), or null.
    static Cell destinationOf(Move move) {
        if (move instanceof CaptureMove cm) return cm.getTargetCell();
        if (move instanceof BlockMove bm) return bm.getTargetCell();
        if (move instanceof MoveOnBoard mob) {
            List<Cell> route = mob.getRoute();
            if (route != null && !route.isEmpty()) {
                return route.get(route.size() - 1);
            }
            return computeDest(mob.getPiece(), mob.getSteps());
        }
        return null;
    }

    // True if the move would put two or more pieces of the mover's colour on one ring cell (a block).
    static boolean formsBlock(Move move) {
        if (move instanceof BlockMove) return true;
        if (move instanceof MoveFromBase mfb) {
            Piece piece = mfb.getPiece();
            return holdsOwnPiece(piece.getBoard().getStartCell(piece.getColour()), piece);
        }
        if (move instanceof MoveOnBoard mob) {
            return holdsOwnPiece(destinationOf(move), mob.getPiece());
        }
        return false;
    }

    private static boolean holdsOwnPiece(Cell cell, Piece mover) {
        return cell != null && cell.getId() < GameConstants.BOARD_SIZE
                && cell.getPieces().stream().anyMatch(p -> p != mover && p.getColour() == mover.getColour());
    }

    // True if the piece shares its ring cell with another piece of its colour.
    static boolean isInBlock(Piece piece) {
        Cell cell = piece.getPosition();
        return cell != null && cell.getId() < GameConstants.BOARD_SIZE
                && cell.getPieces().stream().filter(p -> p.getColour() == piece.getColour()).count() >= 2;
    }

    // T-7: a piece without a capture still needs one before it may enter the home straight.
    static boolean needsCapture(Piece piece) {
        return piece.getCaptureCount() == 0;
    }

    static int remainingToHome(Piece piece) {
        Cell position = piece.getPosition();
        if (position == null) return Integer.MAX_VALUE;
        Colour colour = piece.getColour();
        if (piece.getState() instanceof InHomeStraightState) {
            Board board = piece.getBoard();
            int index = board == null ? 0 : board.getHomeStraight(colour).indexOf(position);
            return GameConstants.HOME_STRAIGHT_LENGTH - 1 - Math.max(index, 0);
        }
        if (piece.getDirection() == null) return Integer.MAX_VALUE;
        int approach = approachCellId(colour);
        int ahead = piece.getDirection() == Direction.CLOCKWISE ? approach - position.getId() : position.getId() - approach;
        return Math.floorMod(ahead, GameConstants.BOARD_SIZE) + GameConstants.HOME_STRAIGHT_LENGTH;
    }

    private static int approachCellId(Colour colour) {
        return switch (colour) {
            case YELLOW -> GameConstants.YELLOW_APPROACH_CELL_ID;
            case BLUE   -> GameConstants.BLUE_APPROACH_CELL_ID;
            case RED    -> GameConstants.RED_APPROACH_CELL_ID;
            case GREEN  -> GameConstants.GREEN_APPROACH_CELL_ID;
        };
    }
}
