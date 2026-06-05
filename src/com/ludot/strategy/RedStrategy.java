package com.ludot.strategy;

import com.ludot.GameConstants;
import com.ludot.command.BlockCaptureMove;
import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Aggressive strategy: capture > get a piece on the board > move closest to an opponent > avoid blocks.
public class RedStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        // 1. Capture takes absolute priority.
        //    S-R2: among multiple captures, prefer the one targeting the opponent closest to its own home.
        List<Move> captures = validMoves.stream()
                .filter(m -> m instanceof CaptureMove)
                .collect(Collectors.toList());
        if (!captures.isEmpty()) {
            return captures.stream()
                    .min(Comparator.comparingInt(m -> distVictimToHome((CaptureMove) m)))
                    .orElse(captures.get(0));
        }

        long onBoard = pieces.stream().filter(p -> p.getPosition() != null).count();

        // 2. S-R3: maintain exactly one piece on the standard path.
        //    Only exit base when no piece is currently on the board.
        if (onBoard == 0) {
            Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);
            if (fromBase.isPresent()) {
                return fromBase.get();
            }
        }

        // 3. Move the piece that is currently closest to any opponent (non-block board moves)
        List<Move> boardMoves = validMoves.stream()
                .filter(m -> m instanceof MoveOnBoard)
                .collect(Collectors.toList());
        if (!boardMoves.isEmpty() && gameState != null && !pieces.isEmpty()) {
            Colour colour = pieces.get(0).getColour();
            List<Piece> opponents = gameState.getOpponentPieces(colour);
            if (!opponents.isEmpty()) {
                return boardMoves.stream()
                        .min(Comparator.comparingInt(m -> minDistToOpponent(m, opponents)))
                        .orElse(boardMoves.get(0));
            }
            return boardMoves.get(0);
        }

        // 4. Avoid creating a block — prefer any non-block, non-base-exit move.
        //    S-R3: exclude MoveFromBase here since onBoard >= 1 at this point; step 2 handles onBoard == 0.
        Optional<Move> nonBlock = validMoves.stream()
                .filter(m -> !(m instanceof BlockMove)
                        && !(m instanceof BlockCaptureMove)
                        && !(m instanceof MoveFromBase))
                .findFirst();
        return nonBlock.orElseGet(() -> StrategyHelper.firstOf(validMoves));
    }

    // S-R2: distance from the capture victim to its own approach cell (smaller = closer to winning).
    private int distVictimToHome(CaptureMove cm) {
        Piece victim = cm.getVictim();
        if (victim.getPosition() == null) return Integer.MAX_VALUE;
        int id = victim.getPosition().getId();
        int approachId = approachCellId(victim.getColour());
        Direction dir = victim.getDirection();
        if (dir == Direction.CLOCKWISE) {
            return (approachId - id + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        } else if (dir == Direction.COUNTER_CLOCKWISE) {
            return (id - approachId + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        }
        return StrategyHelper.circularDist(id, approachId);
    }

    private int approachCellId(Colour colour) {
        return switch (colour) {
            case YELLOW -> GameConstants.YELLOW_APPROACH_CELL_ID;
            case BLUE   -> GameConstants.BLUE_APPROACH_CELL_ID;
            case RED    -> GameConstants.RED_APPROACH_CELL_ID;
            case GREEN  -> GameConstants.GREEN_APPROACH_CELL_ID;
        };
    }

    private int minDistToOpponent(Move m, List<Piece> opponents) {
        Cell dest = destinationOf(m);
        if (dest == null) {
            return Integer.MAX_VALUE;
        }
        return opponents.stream()
                .filter(op -> op.getPosition() != null)
                .mapToInt(op -> StrategyHelper.circularDist(dest.getId(), op.getPosition().getId()))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private Cell destinationOf(Move m) {
        if (m instanceof CaptureMove cm)   return cm.getTargetCell();
        if (m instanceof MoveOnBoard mob)  return StrategyHelper.computeDest(mob.getPiece(), mob.getSteps());
        return null;
    }
}
