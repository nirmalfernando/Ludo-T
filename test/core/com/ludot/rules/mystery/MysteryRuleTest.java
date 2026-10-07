package com.ludot.rules.mystery;

import com.ludot.TestBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MysteryRule (T-10/11)")
class MysteryRuleTest {

    private Board board;
    @BeforeEach  void setUp()    { board = TestBoard.create(); }
        @Test @DisplayName("T-10: shouldSpawn is false before two rounds have passed") void noSpawnEarly() {
        MysteryRule rule = new MysteryRule(new FixedRandomProvider());
        assertFalse(rule.shouldSpawn(0));
        assertFalse(rule.shouldSpawn(1));
    }
    @Test @DisplayName("T-10: shouldSpawn is true once two rounds have passed") void spawnAfterRoundTwo() {
        MysteryRule rule = new MysteryRule(new FixedRandomProvider());
        assertTrue(rule.shouldSpawn(2));
        assertTrue(rule.shouldSpawn(3));
    }
    @Test @DisplayName("getRandomSpawnCell never repeats last location") void noConsecutiveRepeat() {
        FixedRandomProvider fp = new FixedRandomProvider().addCells(5, 5, 10);
        MysteryRule rule = new MysteryRule(fp);
        Cell first  = rule.getRandomSpawnCell(board);
        Cell second = rule.getRandomSpawnCell(board);
        assertNotEquals(first.getId(), second.getId()); // second call should skip cell 5 and use 10
    }
    @Test @DisplayName("resolveTeleport maps roll 1 to ALPHA") void roll1IsAlpha() {
        FixedRandomProvider fp = new FixedRandomProvider().addMysteryEffects(1);
        assertEquals(TeleportDestination.ALPHA,
            new MysteryRule(fp).resolveTeleport(new Piece("R1", Colour.RED, board), board));
    }
    @Test @DisplayName("resolveTeleport maps roll 2 to BETA") void roll2IsBeta() {
        assertEquals(TeleportDestination.BETA,
            new MysteryRule(new FixedRandomProvider().addMysteryEffects(2))
                .resolveTeleport(new Piece("R1", Colour.RED, board), board));
    }

    @Test @DisplayName("resolveTeleport maps roll 3 to GAMMA") void roll3IsGamma() {
        assertEquals(TeleportDestination.GAMMA,
            new MysteryRule(new FixedRandomProvider().addMysteryEffects(3))
                .resolveTeleport(new Piece("R1", Colour.RED, board), board));
    }

    @Test @DisplayName("resolveTeleport maps roll 4 to BASE") void roll4IsBase() {
        assertEquals(TeleportDestination.BASE,
            new MysteryRule(new FixedRandomProvider().addMysteryEffects(4))
                .resolveTeleport(new Piece("R1", Colour.RED, board), board));
    }

    @Test @DisplayName("resolveTeleport maps roll 5 to START_CELL") void roll5IsStartCell() {
        assertEquals(TeleportDestination.START_CELL,
            new MysteryRule(new FixedRandomProvider().addMysteryEffects(5))
                .resolveTeleport(new Piece("R1", Colour.RED, board), board));
    }

    @Test @DisplayName("resolveTeleport maps roll 6 to APPROACH_CELL") void roll6IsApproach() {
        FixedRandomProvider fp = new FixedRandomProvider().addMysteryEffects(6);
        assertEquals(TeleportDestination.APPROACH_CELL,
            new MysteryRule(fp).resolveTeleport(new Piece("R1", Colour.RED, board), board));
    }
}