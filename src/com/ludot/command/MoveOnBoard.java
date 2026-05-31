package com.ludot.command;

import com.ludot.model.AtHomeState;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.InHomeStraightState;
import com.ludot.model.Piece;
import java.util.List;

public class MoveOnBoard implements Move {

    private final Piece piece;
    private final int steps;

    public MoveOnBoard(Piece piece, int steps) {
        this.piece = piece;
        this.steps = steps;
    }

    @Override
    public MoveResult execute() {
        if (piece.getState() instanceof InHomeStraightState) {
            moveInHomeStraight();
        } else {
            piece.move(steps); // circular board navigation handled inside Piece
        }
        return new MoveResult(true, null, false, false, describe());
    }

    // linear forward movement within the colour-specific home straight
    private void moveInHomeStraight() {
        List<Cell> homeStraight = Board.getInstance().getHomeStraight(piece.getColour());
        int currentIndex = homeStraight.indexOf(piece.getPosition());
        Cell target = homeStraight.get(currentIndex + steps);

        piece.getPosition().removePiece(piece);
        target.addPiece(piece);
        piece.setPosition(target);

        // last cell in the home straight is the HOME cell
        if (currentIndex + steps == homeStraight.size() - 1) {
            piece.setState(new AtHomeState());
        }
    }

    @Override
    public String describe() {
        return piece.getName() + " moves " + steps + " steps";
    }

    public Piece getPiece() { return piece; }
    public int getSteps() { return steps; }
}
