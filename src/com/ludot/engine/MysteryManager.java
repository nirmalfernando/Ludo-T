package com.ludot.engine;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.rules.mystery.MysteryRule;
import java.util.Optional;

/**
 * Manages the mystery-cell lifecycle including spawning, landing resolution, and expiry tracking.
 * Delegates spawn-location selection and teleport resolution to MysteryRule; implements Rules T-10, T-11, and T-15.
 */
public class MysteryManager {

    private static final int LIFESPAN = GameConstants.MYSTERY_LIFESPAN;

    private Cell currentCell;
    private int roundsActive;
    private final MysteryRule mysteryRule;
    private final Board board;

    /** Constructs a MysteryManager backed by the given random source and board. */
    public MysteryManager(RandomProvider random, Board board) {
        this.mysteryRule = new MysteryRule(random);
        this.board = board;
        this.currentCell = null;
        this.roundsActive = 0;
    }

    /** Spawns a mystery cell after round 2 on a randomly selected, non-repeated empty cell (Rule T-10). */
    public void spawnMystery(int roundCount) {
        if (currentCell != null || !mysteryRule.shouldSpawn(roundCount)) {
            return;
        }
        currentCell = mysteryRule.getRandomSpawnCell(board);
        currentCell.setMysteryCell(true);
        roundsActive = 0;
    }

    /** Resolves a mystery-cell landing into a TeleportDestination and clears the active cell (Rule T-11). */
    public Optional<TeleportDestination> handleLanding(Piece piece) {
        if (currentCell == null) {
            return Optional.empty();
        }
        TeleportDestination dest = mysteryRule.resolveTeleport(piece, board);
        currentCell.setMysteryCell(false);
        currentCell = null;
        roundsActive = 0;
        return Optional.of(dest);
    }

    /** Returns true if a mystery cell is currently active on the board. */
    public boolean isActive() {
        return currentCell != null;
    }

    /** Advances the mystery-cell lifespan counter and clears the cell once it expires (Rule T-15). */
    public void tick() {
        if (currentCell == null) {
            return;
        }
        roundsActive++;
        if (roundsActive >= LIFESPAN) {
            currentCell.setMysteryCell(false);
            currentCell = null;
            roundsActive = 0;
        }
    }

    /** Returns the currently active mystery cell, or null if none is present. */
    public Cell getCurrentCell() {
        return currentCell;
    }

    /** Returns the number of rounds remaining before the active mystery cell expires. */
    public int getRoundsLeft() {
        return Math.max(0, LIFESPAN - roundsActive);
    }

    /** Returns the total configured lifespan in rounds for a mystery cell. */
    public int getLifespan() {
        return LIFESPAN;
    }
}
