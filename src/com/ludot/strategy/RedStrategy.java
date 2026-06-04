package com.ludot.strategy;

import com.ludot.command.BlockCaptureMove;
import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
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
        // 1. Capture takes absolute priority
        Optional<Move> capture = StrategyHelper.findCapture(validMoves);
        if (capture.isPresent()) return capture.get();

        long onBoard = pieces.stream().filter(p -> p.getPosition() != null).count();

        // 2. No pieces on board — exit base to maintain at least one on the path
        if (onBoard == 0) {
            Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);
            if (fromBase.isPresent()) return fromBase.get();
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

        // 4. Avoid creating a block — prefer any non-block move, else take whatever is available
        Optional<Move> nonBlock = validMoves.stream()
                .filter(m -> !(m instanceof BlockMove) && !(m instanceof BlockCaptureMove))
                .findFirst();
        return nonBlock.orElseGet(() -> StrategyHelper.firstOf(validMoves));
    }

    private int minDistToOpponent(Move m, List<Piece> opponents) {
        Cell dest = destinationOf(m);
        if (dest == null) return Integer.MAX_VALUE;
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
