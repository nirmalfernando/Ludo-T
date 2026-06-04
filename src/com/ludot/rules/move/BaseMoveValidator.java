package com.ludot.rules.move;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.Piece;
import java.util.List;
import java.util.Optional;

public abstract class BaseMoveValidator {

    protected final RandomProvider random;

    
    public BaseMoveValidator(RandomProvider random) {
        this.random = random;
    }

    
    public final boolean isValidMove(Piece piece, int steps) {
        return getDestinationCell(piece, steps).isPresent();
    }

    
    public Optional<Cell> getDestinationCell(Piece piece, int steps) {
        if (piece.getPosition() == null) return Optional.empty();

        if (piece.getState() instanceof InHomeStraightState) {
            List<Cell> hs = Board.getInstance().getHomeStraight(piece.getColour());
            int idx = hs.indexOf(piece.getPosition());
            int target = idx + steps;
            if (target >= hs.size()) return Optional.empty(); // overshooting home is not allowed
            return Optional.of(hs.get(target));
        }

        // standard 52-cell circular path
        int id = piece.getPosition().getId();
        int newId = piece.getDirection() == Direction.CLOCKWISE
                ? (id + steps) % GameConstants.BOARD_SIZE
                : ((id - steps) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        return Optional.of(Board.getInstance().getCell(newId));
    }

    
    public Direction assignDirection(Piece piece) {
        return Direction.CLOCKWISE;
    }

    
    public boolean canEnterHomeStraight(Piece piece) {
        return piece.canEnterHomeStraight();
    }
}
