package com.ludot.engine;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.rules.mystery.MysteryRule;
import java.util.Optional;

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

    
    public void spawnMystery(int roundCount) {
        if (currentCell != null || !mysteryRule.shouldSpawn(roundCount)) return;
        currentCell = mysteryRule.getRandomSpawnCell(board);
        currentCell.setMysteryCell(true);
        roundsActive = 0;
    }

    
    public Optional<TeleportDestination> handleLanding(Piece piece) {
        if (currentCell == null) return Optional.empty();
        TeleportDestination dest = mysteryRule.resolveTeleport(piece, board);
        currentCell.setMysteryCell(false);
        currentCell = null;
        roundsActive = 0;
        return Optional.of(dest);
    }

    
    public boolean isActive() {
        return currentCell != null;
    }

    
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
