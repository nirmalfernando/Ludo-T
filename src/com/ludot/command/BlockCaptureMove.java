package com.ludot.command;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import java.util.List;

public class BlockCaptureMove implements Move {

    private final List<Piece> attackers;
    private final List<Piece> victims;
    private final Cell targetCell;
    private final int steps;

    
    public BlockCaptureMove(List<Piece> attackers, List<Piece> victims, Cell targetCell, int steps) {
        this.attackers = attackers;
        this.victims = victims;
        this.targetCell = targetCell;
        this.steps = steps;
    }

    @Override
    public MoveResult execute() {
        Cell sourceCell = attackers.get(0).getPosition();
        for (Piece attacker : attackers) {
            sourceCell.removePiece(attacker);
            targetCell.addPiece(attacker);
            attacker.setPosition(targetCell);
            attacker.incrementCaptureCount();
        }
        // Victim reset is intentionally NOT done here — MoveExecutor delegates to CaptureRule
        // so that rule-variant-specific reset logic (e.g., T-9 full reset) applies correctly.
        return new MoveResult(true, null, false, true, describe());
    }

    @Override
    public String describe() {
        return "Block of " + attackers.size() + " captures opposing block";
    }

    
    public List<Piece> getAttackers() { return attackers; }

    
    public List<Piece> getVictims()   { return victims; }

    
    public Cell getTargetCell()       { return targetCell; }

    
    public int getSteps()             { return steps; }
}
