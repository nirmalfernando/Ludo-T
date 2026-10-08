package com.ludot.command;

import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.state.OnBoardState;
import java.util.List;

final class PieceMovement {

    private PieceMovement() {
    }

    // reaching the approach cell counter-clockwise counts as a pass (T-1)
    static void follow(Piece piece, List<Cell> route) {
        Board board = piece.getBoard();
        Cell approach = board.getApproachCell(piece.getColour());
        for (Cell cell : route) {
            if (cell == approach && piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
                piece.incrementApproachPassCount();
            }
        }
        Cell target = route.get(route.size() - 1);
        piece.getPosition().removePiece(piece);
        target.addPiece(piece);
        piece.setPosition(target);
        List<Cell> straight = board.getHomeStraight(piece.getColour());
        int index = straight.indexOf(target);
        if (index >= 0) {
            if (piece.getState() instanceof OnBoardState onBoard) {
                onBoard.transitionToHomeStraight(piece); // Rule 9: past the approach cell, the piece is in the home straight
            }
            if (piece.getState() instanceof InHomeStraightState inStraight) {
                inStraight.transitionToHomeIfReached(piece, index, straight.size() - 1);
            }
        }
    }
}
