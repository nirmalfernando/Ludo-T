package com.ludot.command;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import java.util.List;

public class BlockMove implements Move {

    private final List<Piece> block;
    private final Cell targetCell;
    private final int steps;
    private final int originalRoll; // stored so the T-4 output line can show "roll X / size"

    
    public BlockMove(List<Piece> block, Cell targetCell, int steps, int originalRoll) {
        this.block = block;
        this.targetCell = targetCell;
        this.steps = steps;
        this.originalRoll = originalRoll;
    }

    @Override
    public MoveResult execute() {
        Cell sourceCell = block.get(0).getPosition();
        for (Piece piece : block) {
            sourceCell.removePiece(piece);
            targetCell.addPiece(piece);
            piece.setPosition(targetCell);
        }
        return new MoveResult(true, null, false, false, describe());
    }

    @Override
    public String describe() {
        return "Block of " + block.size() + " moves " + steps + " steps";
    }

    
    public List<Piece> getBlock() {
        return block;
    }

    
    public Cell getTargetCell() {
        return targetCell;
    }

    
    public int getSteps() {
        return steps;
    }

    
    public int getOriginalRoll() {
        return originalRoll;
    }
}
