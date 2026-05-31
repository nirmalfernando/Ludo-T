package com.ludot.command;

import java.util.List;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Command that moves an entire block of allied pieces together to a target cell (T-5/6).

public class BlockMove implements Move {

    private final List<Piece> block;
    private final Cell targetCell;
    private final int steps;

    public BlockMove(List<Piece> block, Cell targetCell, int steps) {
        this.block = block;
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
