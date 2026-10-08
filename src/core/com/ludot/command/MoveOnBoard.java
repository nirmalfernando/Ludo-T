package com.ludot.command;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;

public class MoveOnBoard implements Move {

    private final Piece piece;
    private final int steps;
    private final List<Cell> route; // worked out by the rules; null when built directly (tests)

    public MoveOnBoard(Piece piece, int steps) {
        this(piece, steps, null);
    }

    public MoveOnBoard(Piece piece, int steps, List<Cell> route) {
        this.piece = piece;
        this.steps = steps;
        this.route = route;
    }

    @Override
    public MoveResult execute() {
        if (route != null) {
            PieceMovement.follow(piece, route);
        } else if (piece.getState() instanceof InHomeStraightState) {
            moveInHomeStraight();
        } else {
            piece.move(steps); // circular board navigation handled inside Piece
        }
        return new MoveResult(true, null, false, false, describe());
    }

    // linear forward movement within the colour-specific home straight
    private void moveInHomeStraight() {
        List<Cell> homeStraight = piece.getBoard().getHomeStraight(piece.getColour());
        int currentIndex = homeStraight.indexOf(piece.getPosition());
        int targetIndex  = currentIndex + steps;
        Cell target = homeStraight.get(targetIndex);
        piece.getPosition().removePiece(piece);
        target.addPiece(piece);
        piece.setPosition(target);
        // InHomeStraightState owns the transition to AtHomeState (State pattern)
        if (piece.getState() instanceof InHomeStraightState hs) {
            hs.transitionToHomeIfReached(piece, targetIndex, homeStraight.size() - 1);
        }
    }

    @Override
    public String describe() {
        return piece.getName() + " moves " + steps + " steps";
    }

    public Piece getPiece() {
        return piece;
    }

    public int getSteps() {
        return steps;
    }

    public List<Cell> getRoute() {
        return route;
    }
}
