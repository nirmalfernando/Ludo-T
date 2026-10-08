package com.ludot.rules.move;

import com.ludot.GameConstants;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.rules.home.BaseHomeEntryRule;
import java.util.ArrayList;
import java.util.List;

public final class PiecePath {

    private PiecePath() {
    }

    // empty when the move is impossible (it would overshoot home: Rule 10 needs an exact roll)
    public static List<Cell> route(Piece piece, int steps, BaseHomeEntryRule homeEntry) {
        return route(piece, steps, homeEntry, piece.getDirection());
    }

    // the same, travelling in the given direction (a piece leaving a block goes back to its original direction, T-5)
    public static List<Cell> route(Piece piece, int steps, BaseHomeEntryRule homeEntry, Direction direction) {
        Board board = piece.getBoard();
        Colour colour = piece.getColour();
        List<Cell> straight = board.getHomeStraight(colour);
        Cell approach = board.getApproachCell(colour);
        List<Cell> visited = new ArrayList<>(steps);
        Cell at = piece.getPosition();
        int straightIndex = straight.indexOf(at); // -1 while on the 52-cell path
        int approachPasses = piece.getApproachPassCount();
        for (int step = 0; step < steps; step++) {
            if (straightIndex >= 0) {
                straightIndex++;
                if (straightIndex >= straight.size()) {
                    return List.of();
                }
                at = straight.get(straightIndex);
            } else if (at == approach && homeEntry.mayEnterHomeStraight(piece, direction, approachPasses)) {
                straightIndex = 0;
                at = straight.get(0);
            } else {
                at = neighbour(board, at, direction);
                if (at == approach && direction == Direction.COUNTER_CLOCKWISE) {
                    approachPasses++;
                }
            }
            visited.add(at);
        }
        return visited;
    }

    public static Cell neighbour(Board board, Cell cell, Direction direction) {
        int size = GameConstants.BOARD_SIZE;
        int id = cell.getId();
        int next = direction == Direction.COUNTER_CLOCKWISE ? (id - 1 + size) % size : (id + 1) % size;
        return board.getCell(next);
    }
}
