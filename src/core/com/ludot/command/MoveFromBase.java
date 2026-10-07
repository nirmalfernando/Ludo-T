package com.ludot.command;

import com.ludot.model.Cell;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.Piece;

public class MoveFromBase implements Move {

    private final Piece piece;
    private final Piece victim; // a lone opponent on the start cell, captured by the arrival (Rule 6); usually null

    
    public MoveFromBase(Piece piece) {
        this(piece, null);
    }

    public MoveFromBase(Piece piece, Piece victim) {
        this.piece = piece;
        this.victim = victim;
    }

    @Override
    public MoveResult execute() {
        Cell startCell = piece.getBoard().getStartCell(piece.getColour());
        startCell.addPiece(piece);
        piece.setPosition(startCell);
        // AtBaseState owns the transition to OnBoardState (State pattern)
        if (piece.getState() instanceof AtBaseState atBase) {
            atBase.onExitedBase(piece);
        }
        return new MoveResult(true, victim, false, victim != null, describe());
    }

    @Override
    public String describe() {
        return piece.getName() + " moves out of base";
    }

    
    public Piece getPiece() {
        return piece;
    }

    /** The lone opponent this arrival captures, or null. */
    public Piece getVictim() {
        return victim;
    }
}
