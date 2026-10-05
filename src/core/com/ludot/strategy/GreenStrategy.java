package com.ludot.strategy;

import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import java.util.List;
import java.util.Optional;

// Blocking strategy: exit base > form/move a blockade > capture > any other move.
public class GreenStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);

        // 1. S-G1: exit base on 6, UNLESS a board move would form a new block this turn.
        //    Block formation is more valuable than adding a fresh piece from base.
        if (fromBase.isPresent()) {
            Optional<Move> blockForming = findBlockFormingMove(validMoves, pieces);
            if (blockForming.isPresent()) {
                return blockForming.get();
            }
            return fromBase.get();
        }

        // 2. Form a new blockade or advance an existing one (T-3/T-4)
        Optional<Move> block = StrategyHelper.findBlock(validMoves);
        if (block.isPresent()) {
            return block.get();
        }

        // 3. Capture if no block opportunity exists
        Optional<Move> capture = StrategyHelper.findCapture(validMoves);
        if (capture.isPresent()) {
            return capture.get();
        }

        // 4. Any remaining move
        return StrategyHelper.firstOf(validMoves);
    }

    // A MoveOnBoard is "block-forming" when its destination already holds a friendly piece.
    private Optional<Move> findBlockFormingMove(List<Move> validMoves, List<Piece> pieces) {
        if (pieces.isEmpty()) return Optional.empty();
        Colour colour = pieces.get(0).getColour();
        return validMoves.stream()
                .filter(m -> m instanceof MoveOnBoard mob && isBlockForming(mob, colour))
                .findFirst();
    }

    private boolean isBlockForming(MoveOnBoard mob, Colour colour) {
        Cell dest = StrategyHelper.computeDest(mob.getPiece(), mob.getSteps());
        if (dest == null) return false;
        return dest.getPieces().stream().anyMatch(p -> p.getColour() == colour);
    }
}
