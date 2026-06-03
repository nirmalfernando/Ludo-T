package com.ludot.engine;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.rules.mystery.MysteryRule;

// Manages the mystery-cell lifecycle: spawning, landing resolution, and expiry tracking (T-10/11/15).
// Delegates spawn-location selection and teleport resolution to MysteryRule.
public class MysteryManager {

    private static final int LIFESPAN = GameConstants.MYSTERY_LIFESPAN;

    private Cell currentCell;
    private int roundsActive;
    private final MysteryRule mysteryRule;
    private final Board board;

    public MysteryManager(RandomProvider random, Board board) {
        this.mysteryRule = new MysteryRule(random);
        this.board = board;
        this.currentCell = null;
        this.roundsActive = 0;
    }

    // T-10: spawn only after round 2, never on the same cell twice in a row
    public void spawnMystery(int roundCount) {
        if (currentCell != null || !mysteryRule.shouldSpawn(roundCount)) return;
        currentCell = mysteryRule.getRandomSpawnCell(board);
        currentCell.setMysteryCell(true);
        roundsActive = 0;
    }

    // T-11: resolve a 1-6 roll into a teleport destination and clear the cell
    public TeleportDestination handleLanding(Piece piece) {
        if (currentCell == null) return null;
        TeleportDestination dest = mysteryRule.resolveTeleport(piece, board);
        currentCell.setMysteryCell(false);
        currentCell = null;
        roundsActive = 0;
        return dest;
    }

    public boolean isActive() {
        return currentCell != null;
    }

    // T-15: advances the active-rounds counter; caller should check isActive() before/after to detect expiry
    public void tick() {
        if (currentCell == null) return;
        roundsActive++;
        if (roundsActive >= LIFESPAN) {
            currentCell.setMysteryCell(false);
            currentCell = null;
            roundsActive = 0;
        }
    }

    public Cell getCurrentCell() { return currentCell; }
    public int getRoundsLeft() { return Math.max(0, LIFESPAN - roundsActive); }
    public int getLifespan() { return LIFESPAN; }
}
