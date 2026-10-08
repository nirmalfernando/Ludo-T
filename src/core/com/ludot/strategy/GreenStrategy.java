package com.ludot.strategy;

import com.ludot.command.BlockCaptureMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class GreenStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);
        if (fromBase.isPresent()) {
            return findBlockFormingMove(validMoves).orElse(fromBase.get());
        }
        return StrategyHelper.findBlock(validMoves)
                .or(() -> blockCaptureNeeded(validMoves))
                .or(() -> captureNeededBy(validMoves, attacker -> !StrategyHelper.isInBlock(attacker)))
                .or(() -> closestToHome(validMoves, piece -> !StrategyHelper.isInBlock(piece)))
                .or(() -> captureNeededBy(validMoves, attacker -> true))
                .or(() -> closestToHome(validMoves, piece -> true))
                .orElseGet(() -> StrategyHelper.firstOf(validMoves));
    }

    // A board move that puts two of Green's pieces on one cell.
    private Optional<Move> findBlockFormingMove(List<Move> validMoves) {
        return validMoves.stream()
                .filter(m -> m instanceof MoveOnBoard && StrategyHelper.formsBlock(m))
                .findFirst();
    }

    private Optional<Move> blockCaptureNeeded(List<Move> validMoves) {
        return validMoves.stream()
                .filter(m -> m instanceof BlockCaptureMove bcm
                        && bcm.getAttackers().stream().anyMatch(StrategyHelper::needsCapture))
                .findFirst();
    }

    private Optional<Move> captureNeededBy(List<Move> validMoves, Predicate<Piece> allowed) {
        return validMoves.stream()
                .filter(m -> m instanceof CaptureMove cm
                        && StrategyHelper.needsCapture(cm.getAttacker()) && allowed.test(cm.getAttacker()))
                .findFirst();
    }

    private Optional<Move> closestToHome(List<Move> validMoves, Predicate<Piece> allowed) {
        return validMoves.stream()
                .filter(m -> m instanceof MoveOnBoard mob && allowed.test(mob.getPiece()))
                .min(Comparator.comparingInt(m -> StrategyHelper.remainingToHome(((MoveOnBoard) m).getPiece())));
    }
}
