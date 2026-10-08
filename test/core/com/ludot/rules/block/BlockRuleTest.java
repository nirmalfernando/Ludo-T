package com.ludot.rules.block;

import com.ludot.TestBoard;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BlockRule (T-3/4/5/6)")
class BlockRuleTest {

    private Board board;
    private final BlockRule rule = new BlockRule();
    @BeforeEach void setUp()    { board = TestBoard.create(); }
        private Cell makeBlock(Colour c) {
        Cell cell = new Cell(5, CellType.STANDARD, null);
        Piece p1 = new Piece("1", c, board); Piece p2 = new Piece("2", c, board);
        cell.addPiece(p1); cell.addPiece(p2);
        p1.setPosition(cell); p2.setPosition(cell);
        return cell;
    }
    @Test @DisplayName("isBlocked true for opponent block on destination") void opponentBlockBlocks() {
        Piece attacker = new Piece("R1", Colour.RED, board);
        Cell dest = makeBlock(Colour.BLUE);
        assertTrue(rule.isBlocked(attacker, dest));
    }
    @Test @DisplayName("isBlocked false for friendly block") void friendlyBlockDoesNotBlock() {
        Piece attacker = new Piece("R1", Colour.RED, board);
        Cell dest = makeBlock(Colour.RED);
        assertFalse(rule.isBlocked(attacker, dest));
    }
    @Test @DisplayName("canFormBlock true when same colour on cell") void canFormBlock() {
        Piece mover = new Piece("R1", Colour.RED, board);
        Cell cell = new Cell(3, CellType.STANDARD, null);
        Piece resident = new Piece("R2", Colour.RED, board);
        cell.addPiece(resident);
        assertTrue(rule.canFormBlock(mover, cell));
    }
    @Test @DisplayName("calcBlockSteps is roll / blockSize floor") void blockStepsFloorDiv() {
        assertEquals(2, rule.calcBlockSteps(5, 2)); // 5/2=2
        assertEquals(1, rule.calcBlockSteps(3, 2)); // 3/2=1
    }
    @Test @DisplayName("calcBlockMoveDir returns direction of furthest-from-home piece (T-4)")
    void blockDirFromFurthest() {
        // so the block should move CCW (the furthest-from-home direction)
        Cell cell10 = board.getCell(10);
        Piece p1 = new Piece("R1", Colour.RED, board);
        Piece p2 = new Piece("R2", Colour.RED, board);
        cell10.addPiece(p1); p1.setPosition(cell10); p1.setDirection(Direction.CLOCKWISE);
        cell10.addPiece(p2); p2.setPosition(cell10); p2.setDirection(Direction.COUNTER_CLOCKWISE);
        assertEquals(Direction.COUNTER_CLOCKWISE, rule.calcBlockMoveDir(List.of(p1, p2)),
                "Block must move in the direction of the piece furthest from its approach cell");
    }

    @Test @DisplayName("calcBlockMoveDir with same-direction block uses that direction")
    void blockDirSameDirection() {
        Cell cell10 = board.getCell(10);
        Piece p1 = new Piece("R1", Colour.RED, board);
        Piece p2 = new Piece("R2", Colour.RED, board);
        cell10.addPiece(p1); p1.setPosition(cell10); p1.setDirection(Direction.CLOCKWISE);
        cell10.addPiece(p2); p2.setPosition(cell10); p2.setDirection(Direction.CLOCKWISE);
        assertEquals(Direction.CLOCKWISE, rule.calcBlockMoveDir(List.of(p1, p2)));
    }
    @Test @DisplayName("forceBreakBlock restores each piece's original direction (and keeps remembering it)") void restoresOriginalDir() {
        Piece p1 = new Piece("R1", Colour.RED, board);
        p1.setOriginalDirection(Direction.CLOCKWISE);
        p1.setDirection(Direction.COUNTER_CLOCKWISE);
        rule.forceBreakBlock(List.of(p1));
        assertEquals(Direction.CLOCKWISE, p1.getDirection());
        assertEquals(Direction.CLOCKWISE, p1.getOriginalDirection());
    }
}