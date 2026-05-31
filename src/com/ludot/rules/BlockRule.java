package com.ludot.rules;

import java.util.List;
import com.ludot.dice.RandomProvider;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;

// Encapsulates all block-related rules: formation, movement, forced-break and direction (T-3/4/5/6).

public class BlockRule {

    private final RandomProvider random;

    public BlockRule(RandomProvider random) {
        this.random = random;
    }

    public boolean isBlocked(Piece piece, Cell cell) {
        return false;
    }

    public boolean canFormBlock(Piece piece, Cell cell) {
        return false;
    }

    public Direction calcBlockMoveDir(List<Piece> block) {
        return null;
    }

    public int calcBlockSteps(int roll, int blockSize) {
        return 0;
    }

    public void forceBreakBlock(List<Piece> block) {
    }

    public Cell getPartialMoveCell(List<Piece> block, Cell targetCell) {
        return null;
    }
}
