package com.ludot.engine;

import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import java.util.HashSet;
import java.util.Set;

// Manages the mystery-cell lifecycle: spawning, landing resolution, and expiry tracking (T-10/11/15).
public class MysteryManager {

    private static final int LIFESPAN = 3; // T-15: mystery cell expires after 3 rounds

    private Cell currentCell;
    private int roundsActive;
    private final Set<Integer> previousLocations;
    private final RandomProvider random;
    private final Board board;

    public MysteryManager(RandomProvider random, Board board) {
        this.random = random;
        this.board = board;
        this.currentCell = null;
        this.roundsActive = 0;
        this.previousLocations = new HashSet<>();
    }

    // T-10: spawn only after round 2, never on the same cell twice in a row
    public void spawnMystery(int roundCount) {
        if (currentCell != null || roundCount <= 2) return;
        int cellId;
        do {
            cellId = random.getRandomCell(52);
        } while (previousLocations.contains(cellId));
        previousLocations.clear();
        previousLocations.add(cellId);
        currentCell = board.getCell(cellId);
        currentCell.setMysteryCell(true);
        roundsActive = 0;
    }

    // T-11: resolve a 1-6 roll into a teleport destination and clear the cell
    public TeleportDestination handleLanding(Piece piece) {
        if (currentCell == null) return null;
        TeleportDestination dest = switch (random.getMysteryEffect()) {
            case 1 -> TeleportDestination.ALPHA;
            case 2 -> TeleportDestination.BETA;
            case 3 -> TeleportDestination.GAMMA;
            case 4 -> TeleportDestination.BASE;
            case 5 -> TeleportDestination.START_CELL;
            default -> TeleportDestination.APPROACH_CELL;
        };
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
}
