package com.ludot.command;

import com.ludot.model.Cell;
import com.ludot.model.Piece;

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
        // move attacker to the target cell
        attacker.getPosition().removePiece(attacker);
        targetCell.addPiece(attacker);
        attacker.setPosition(targetCell);

        // send victim back to base and record the capture
        victim.sendToBase();
        attacker.incrementCaptureCount();

        // grantsBonusRoll = true triggers T-2 bonus roll in TurnManager
        return new MoveResult(true, victim, false, true, describe());
    }

    @Override
    public String describe() {
        return attacker.getName() + " captures " + victim.getName();
    }

    public Piece getAttacker() { return attacker; }
    public Cell getTargetCell() { return targetCell; }
    public int getSteps() { return steps; }
}
