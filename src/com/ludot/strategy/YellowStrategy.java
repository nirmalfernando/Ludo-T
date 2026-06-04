package com.ludot.strategy;

import com.ludot.GameConstants;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import com.ludot.model.state.InHomeStraightState;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Winning strategy: exit base > advance home-straight pieces > capture only if needed for T-7 >
// move the piece closest to the approach cell.
public class YellowStrategy implements PlayerStrategy {

    @Override
    public Move decideMoves(GameState gameState, int roll, List<Piece> pieces, List<Move> validMoves) {
        // 1. Exit base to get more pieces working toward home
        Optional<Move> fromBase = StrategyHelper.findMoveFromBase(validMoves);
        if (fromBase.isPresent()) return fromBase.get();

        // 2. Advance pieces already in the home straight — they are closest to winning
        Optional<Move> homeStraight = StrategyHelper.findHomeStraight(validMoves);
        if (homeStraight.isPresent()) return homeStraight.get();

        // 3. Capture only if the attacker still needs one capture to satisfy T-7 (captureCount == 0)
        Optional<Move> capture = StrategyHelper.findCapture(validMoves);
        if (capture.isPresent() && capture.get() instanceof CaptureMove cm
                && cm.getAttacker().getCaptureCount() == 0) {
            return capture.get();
        }

        // 4. Move the on-board piece that is nearest to its approach cell
        if (!pieces.isEmpty()) {
            Colour colour = pieces.get(0).getColour();
            List<Move> boardMoves = validMoves.stream()
                    .filter(m -> m instanceof MoveOnBoard mob
                            && !(mob.getPiece().getState() instanceof InHomeStraightState))
                    .collect(Collectors.toList());
            if (!boardMoves.isEmpty()) {
                return boardMoves.stream()
                        .min(Comparator.comparingInt(
                                m -> distToApproach(((MoveOnBoard) m).getPiece(), colour)))
                        .orElse(boardMoves.get(0));
            }
        }

        return StrategyHelper.firstOf(validMoves);
    }

    // Remaining path length to the approach cell in the piece's travel direction.
    private int distToApproach(Piece piece, Colour colour) {
        if (piece.getPosition() == null || piece.getDirection() == null) return Integer.MAX_VALUE;
        int pieceId    = piece.getPosition().getId();
        int approachId = approachCellId(colour);
        if (piece.getDirection() == Direction.CLOCKWISE) {
            return (approachId - pieceId + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        } else {
            return (pieceId - approachId + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        }
    }

    private int approachCellId(Colour colour) {
        return switch (colour) {
            case YELLOW -> GameConstants.YELLOW_APPROACH_CELL_ID;
            case BLUE   -> GameConstants.BLUE_APPROACH_CELL_ID;
            case RED    -> GameConstants.RED_APPROACH_CELL_ID;
            case GREEN  -> GameConstants.GREEN_APPROACH_CELL_ID;
        };
    }
}
