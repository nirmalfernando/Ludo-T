package com.ludot.engine;

import com.ludot.TestBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MysteryManager (T-10/11/15)")
class MysteryManagerTest {
    private Board board;
    @BeforeEach void setUp()    { board = TestBoard.create(); }
        @Test @DisplayName("isActive false initially") void notActiveInitially() {
        assertFalse(new MysteryManager(new FixedRandomProvider(), board).isActive());
    }
    @Test @DisplayName("spawnMystery does nothing before round 3") void noSpawnBeforeRound3() {
        MysteryManager m = new MysteryManager(new FixedRandomProvider().addCells(5), board);
        m.spawnMystery(1); assertFalse(m.isActive());
        m.spawnMystery(2); assertFalse(m.isActive());
    }
    @Test @DisplayName("spawnMystery marks cell after round 2") void spawnsAfterRound2() {
        MysteryManager m = new MysteryManager(new FixedRandomProvider().addCells(5), board);
        m.spawnMystery(3);
        assertTrue(m.isActive());
        assertTrue(m.getCurrentCell().isMysteryCell());
    }
    @Test @DisplayName("tick expires cell after 4 rounds (MYSTERY_LIFESPAN)") void expiresAfterFourRounds() {
        MysteryManager m = new MysteryManager(new FixedRandomProvider().addCells(5), board);
        m.spawnMystery(3);
        m.tick(); m.tick(); m.tick();
        assertTrue(m.isActive(), "Cell should still be active after 3 ticks");
        m.tick();
        assertFalse(m.isActive(), "Cell should expire after 4 ticks");
    }
    @Test @DisplayName("handleLanding returns empty Optional and has no side effects when no mystery is active")
    void handleLandingReturnsNullWhenInactive() {
        MysteryManager m = new MysteryManager(new FixedRandomProvider(), board);
        // no spawnMystery call — manager is inactive
        assertTrue(m.handleLanding(new Piece("R1", Colour.RED, board)).isEmpty(),
                "handleLanding must return empty Optional when no mystery cell is active");
        assertFalse(m.isActive(), "Manager must remain inactive after landing on nothing");
    }

    @Test @DisplayName("handleLanding clears cell and returns destination") void handlesLanding() {
        FixedRandomProvider fp = new FixedRandomProvider().addCells(5).addMysteryEffects(1);
        MysteryManager m = new MysteryManager(fp, board);
        m.spawnMystery(3);
        TeleportDestination dest = m.handleLanding(new Piece("R1", Colour.RED, board)).orElse(null);
        assertEquals(TeleportDestination.ALPHA, dest);
        assertFalse(m.isActive());
    }
}