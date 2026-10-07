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

        // 2. S-R3: Red keeps one piece on the path and brings another out only when it cannot capture
        //    by moving six cells, which is the case here because captures were handled above.
        Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);
        if (fromBase.isPresent()) {
            return fromBase.get();
        }

        // 3. Otherwise move the piece closest to an opponent, avoiding block-forming moves unless unavoidable.
        List<Move> boardMoves = validMoves.stream()
                .filter(m -> m instanceof MoveOnBoard)
                .collect(Collectors.toList());
        List<Move> withoutBlock = boardMoves.stream()
                .filter(m -> !StrategyHelper.formsBlock(m))
                .collect(Collectors.toList());
        List<Move> candidates = withoutBlock.isEmpty() ? boardMoves : withoutBlock;
        if (!candidates.isEmpty()) {
            return closestToAnOpponent(gameState, pieces, candidates);
        }
        return validMoves.stream()
                .filter(m -> !(m instanceof BlockMove) && !(m instanceof BlockCaptureMove))
                .findFirst()
                .orElseGet(() -> StrategyHelper.firstOf(validMoves));
    }

    private Move closestToAnOpponent(GameState gameState, List<Piece> pieces, List<Move> candidates) {
        if (gameState == null || pieces.isEmpty()) {
            return candidates.get(0);
        }
        List<Piece> opponents = gameState.getOpponentPieces(pieces.get(0).getColour());
        if (opponents.isEmpty()) {
            return candidates.get(0);
        }
        return candidates.stream()
                .min(Comparator.comparingInt(m -> minDistToOpponent(m, opponents)))
                .orElse(candidates.get(0));
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
        Cell dest = StrategyHelper.destinationOf(m);
        if (dest == null) {
            return Integer.MAX_VALUE;
        }
        return opponents.stream()
                .filter(op -> op.getPosition() != null)
                .mapToInt(op -> StrategyHelper.circularDist(dest.getId(), op.getPosition().getId()))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

}
