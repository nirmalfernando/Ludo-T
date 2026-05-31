package com.ludot.engine;

import java.util.Set;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Manages the mystery-cell lifecycle: spawning, landing resolution, and expiry tracking (T-10/11/15).

public class MysteryManager {

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
        this.previousLocations = new java.util.HashSet<>();
    }

    public void spawnMystery(int roundCount) {
    }

    public TeleportDestination handleLanding(Piece piece) {
        return null;
    }

    public boolean isActive() {
        return false;
    }

    public void tick() {
    }
}
