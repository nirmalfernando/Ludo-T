package com.ludot.rules.mystery;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.factory.CellFactory;
import com.ludot.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MysteryRule (T-10/11)")
class MysteryRuleTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); CellFactory.initializeBoard(Board.getInstance()); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("shouldSpawn false for rounds 1 and 2") void noSpawnEarly() {
        MysteryRule rule = new MysteryRule(new FixedRandomProvider());
        assertFalse(rule.shouldSpawn(1));
        assertFalse(rule.shouldSpawn(2));
    }
    @Test @DisplayName("shouldSpawn true after round 2") void spawnAfterRoundTwo() {
        assertTrue(new MysteryRule(new FixedRandomProvider()).shouldSpawn(3));
    }
    @Test @DisplayName("getRandomSpawnCell never repeats last location") void noConsecutiveRepeat() {
        FixedRandomProvider fp = new FixedRandomProvider().addCells(5, 5, 10);
        MysteryRule rule = new MysteryRule(fp);
        Cell first  = rule.getRandomSpawnCell(Board.getInstance());
        Cell second = rule.getRandomSpawnCell(Board.getInstance());
        assertNotEquals(first.getId(), second.getId()); // second call should skip cell 5 and use 10
    }
    @Test @DisplayName("resolveTeleport maps roll 1 to ALPHA") void roll1IsAlpha() {
        FixedRandomProvider fp = new FixedRandomProvider().addMysteryEffects(1);
        assertEquals(TeleportDestination.ALPHA,
            new MysteryRule(fp).resolveTeleport(new Piece("R1", Colour.RED), Board.getInstance()));
    }
    @Test @DisplayName("resolveTeleport maps roll 6 to APPROACH_CELL") void roll6IsApproach() {
        FixedRandomProvider fp = new FixedRandomProvider().addMysteryEffects(6);
        assertEquals(TeleportDestination.APPROACH_CELL,
            new MysteryRule(fp).resolveTeleport(new Piece("R1", Colour.RED), Board.getInstance()));
    }
}