package com.ludot.command;

import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.Piece;

public class MoveFromBase implements Move {

    private final Piece piece;

    
    public MoveFromBase(Piece piece) {
        this.piece = piece;
    }

    @Override
    public MoveResult execute() {
        Cell startCell = Board.getInstance().getStartCell(piece.getColour());
        startCell.addPiece(piece);
        piece.setPosition(startCell);
        // AtBaseState owns the transition to OnBoardState (State pattern)
        if (piece.getState() instanceof AtBaseState atBase) {
            atBase.onExitedBase(piece);
        }
        return new MoveResult(true, null, false, false, describe());
    }

    @Override
    public String describe() {
        return piece.getName() + " moves out of base";
    }

    
    public Piece getPiece() { return piece; }
}
