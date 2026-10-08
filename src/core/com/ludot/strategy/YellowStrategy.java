package com.ludot.strategy;

import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// it never captures more than it needs for the home straight
public class YellowStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);
        if (fromBase.isPresent()) {
            return fromBase.get();
        }
        Optional<Move> neededCapture = capturesByPiecesThatNeedOne(validMoves);
        if (neededCapture.isPresent()) {
            return neededCapture.get();
        }
        return validMoves.stream()
                .filter(m -> m instanceof MoveOnBoard)
                .min(Comparator.comparingInt(m -> StrategyHelper.remainingToHome(((MoveOnBoard) m).getPiece())))
                .orElseGet(() -> StrategyHelper.firstOf(validMoves));
    }

    private Optional<Move> capturesByPiecesThatNeedOne(List<Move> validMoves) {
        return validMoves.stream()
                .filter(m -> m instanceof CaptureMove cm && StrategyHelper.needsCapture(cm.getAttacker()))
                .findFirst();
    }
}
