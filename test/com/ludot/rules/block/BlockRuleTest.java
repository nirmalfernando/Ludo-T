package com.ludot.rules.block;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.model.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BlockRule (T-3/4/5/6)")
class BlockRuleTest {
    private final BlockRule rule = new BlockRule(new FixedRandomProvider());
    private Cell makeBlock(Colour c) {
        Cell cell = new Cell(5, CellType.STANDARD, null);
        Piece p1 = new Piece("1", c); Piece p2 = new Piece("2", c);
        cell.addPiece(p1); cell.addPiece(p2);
        p1.setPosition(cell); p2.setPosition(cell);
        return cell;
    }
    @Test @DisplayName("isBlocked true for opponent block on destination") void opponentBlockBlocks() {
        Piece attacker = new Piece("R1", Colour.RED);
        Cell dest = makeBlock(Colour.BLUE);
        assertTrue(rule.isBlocked(attacker, dest));
    }
    @Test @DisplayName("isBlocked false for friendly block") void friendlyBlockDoesNotBlock() {
        Piece attacker = new Piece("R1", Colour.RED);
        Cell dest = makeBlock(Colour.RED);
        assertFalse(rule.isBlocked(attacker, dest));
    }
    @Test @DisplayName("canFormBlock true when same colour on cell") void canFormBlock() {
        Piece mover = new Piece("R1", Colour.RED);
        Cell cell = new Cell(3, CellType.STANDARD, null);
        Piece resident = new Piece("R2", Colour.RED);
        cell.addPiece(resident);
        assertTrue(rule.canFormBlock(mover, cell));
    }
    @Test @DisplayName("calcBlockSteps is roll / blockSize floor") void blockStepsFloorDiv() {
        assertEquals(2, rule.calcBlockSteps(5, 2)); // 5/2=2
        assertEquals(1, rule.calcBlockSteps(3, 2)); // 3/2=1
    }
    @Test @DisplayName("calcBlockMoveDir uses direction of first piece") void blockDirFromFirst() {
        Piece p1 = new Piece("R1", Colour.RED);
        Piece p2 = new Piece("R2", Colour.RED);
        p1.setDirection(Direction.COUNTER_CLOCKWISE);
        assertEquals(Direction.COUNTER_CLOCKWISE, rule.calcBlockMoveDir(List.of(p1, p2)));
    }
    @Test @DisplayName("forceBreakBlock restores original directions") void restoresOriginalDir() {
        Piece p1 = new Piece("R1", Colour.RED);
        p1.setOriginalDirection(Direction.CLOCKWISE);
        p1.setDirection(Direction.COUNTER_CLOCKWISE);
        rule.forceBreakBlock(List.of(p1));
        assertEquals(Direction.CLOCKWISE, p1.getDirection());
        assertNull(p1.getOriginalDirection());
    }
}