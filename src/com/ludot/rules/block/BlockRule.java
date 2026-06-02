package com.ludot.rules.block;

import com.ludot.dice.RandomProvider;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import java.util.List;

// T-3/4/5/6: encapsulates all block formation, movement, and forced-break logic.
public class BlockRule {

    private final RandomProvider random;

    public BlockRule(RandomProvider random) {
        this.random = random;
    }

    // T-3: destination cell has an opponent block — movement is blocked
    public boolean isBlocked(Piece piece, Cell cell) {
        return cell.hasBlock() && cell.getPieces().get(0).getColour() != piece.getColour();
    }

    // T-4: landing on a same-colour cell forms a block
    public boolean canFormBlock(Piece piece, Cell cell) {
        if (!cell.isOccupied()) return false;
        return cell.getPieces().get(0).getColour() == piece.getColour();
    }

    // T-5: block moves in the direction of its constituent pieces
    public Direction calcBlockMoveDir(List<Piece> block) {
        return block.get(0).getDirection();
    }

    // T-5: block steps = roll / blockSize rounded down
    public int calcBlockSteps(int roll, int blockSize) {
        return roll / blockSize;
    }

    // T-6: on triple-six, restore each piece's original direction and dissolve the block
    public void forceBreakBlock(List<Piece> block) {
        for (Piece piece : block) {
            if (piece.getOriginalDirection() != null) {
                piece.setDirection(piece.getOriginalDirection());
                piece.setOriginalDirection(null);
            }
        }
    }

    public Cell getPartialMoveCell(List<Piece> block, Cell targetCell) {
        return targetCell;
    }
}
