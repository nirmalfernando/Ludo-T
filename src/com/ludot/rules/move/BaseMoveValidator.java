package com.ludot.rules.move;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.Piece;
import java.util.List;

// Template Method — base defines the algorithm; subclasses override direction and home-entry hooks.
public abstract class BaseMoveValidator {

    protected final RandomProvider random;

    public BaseMoveValidator(RandomProvider random) {
        this.random = random;
    }

    public final boolean isValidMove(Piece piece, int steps) {
        return getDestinationCell(piece, steps) != null;
    }

    public Cell getDestinationCell(Piece piece, int steps) {
        if (piece.getPosition() == null) return null;

        if (piece.getState() instanceof InHomeStraightState) {
            List<Cell> hs = Board.getInstance().getHomeStraight(piece.getColour());
            int idx = hs.indexOf(piece.getPosition());
            int target = idx + steps;
            if (target >= hs.size()) return null; // overshooting home is not allowed
            return hs.get(target);
        }

        // standard 52-cell circular path
        int id = piece.getPosition().getId();
        int newId = piece.getDirection() == Direction.CLOCKWISE
                ? (id + steps) % GameConstants.BOARD_SIZE
                : ((id - steps) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        return Board.getInstance().getCell(newId);
    }

    // base game: always clockwise — public so RuleEngine (different sub-package) can call it
    public Direction assignDirection(Piece piece) {
        return Direction.CLOCKWISE;
    }

    // base game: piece just needs to be at its approach cell
    public boolean canEnterHomeStraight(Piece piece) {
        return piece.canEnterHomeStraight();
    }
}
