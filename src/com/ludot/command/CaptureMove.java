package com.ludot.command;

import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Command that moves an attacker piece to a cell and captures the victim piece there.

public class CaptureMove implements Move {

    private final Piece attacker;
    private final Piece victim;
    private final Cell targetCell;
    private final int steps;

    public CaptureMove(Piece attacker, Piece victim, Cell targetCell, int steps) {
        this.attacker = attacker;
        this.victim = victim;
        this.targetCell = targetCell;
        this.steps = steps;
    }

    @Override
    public MoveResult execute() {
        return null;
    }

    @Override
    public String describe() {
        return null;
    }
}
