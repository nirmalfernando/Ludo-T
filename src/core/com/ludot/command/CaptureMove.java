package com.ludot.command;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import java.util.List;

public class CaptureMove implements Move {

    private final Piece attacker;
    private final Piece victim;
    private final Cell targetCell;
    private final int steps;
    private final List<Cell> route; // null when built directly (tests)

    public CaptureMove(Piece attacker, Piece victim, Cell targetCell, int steps) {
        this(attacker, victim, targetCell, steps, null);
    }

    public CaptureMove(Piece attacker, Piece victim, Cell targetCell, int steps, List<Cell> route) {
        this.attacker = attacker;
        this.victim = victim;
        this.targetCell = targetCell;
        this.steps = steps;
        this.route = route;
    }

    @Override
    public MoveResult execute() {
        if (route != null) {
            PieceMovement.follow(attacker, route);
        } else {
            attacker.getPosition().removePiece(attacker);
            targetCell.addPiece(attacker);
            attacker.setPosition(targetCell);
        }
        return new MoveResult(true, victim, false, true, describe());
    }

    @Override
    public String describe() {
        return attacker.getName() + " captures " + victim.getName();
    }

    public Piece getAttacker() {
        return attacker;
    }

    public Piece getVictim() {
        return victim;
    }

    public Cell getTargetCell() {
        return targetCell;
    }

    public int getSteps() {
        return steps;
    }

    public List<Cell> getRoute() {
        return route;
    }
}
