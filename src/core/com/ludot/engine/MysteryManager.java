package com.ludot.engine;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.rules.mystery.MysteryRule;
import java.util.Optional;

// delegates spawn-location selection and teleport resolution to MysteryRule; implements Rules T-10, T-11, and T-15
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

    // spawns a mystery cell after round 2 on a randomly selected, non-repeated empty cell (Rule T-10)
    public void spawnMystery(int roundCount) {
        if (currentCell != null || !mysteryRule.shouldSpawn(roundCount)) {
            return;
        }
        currentCell = mysteryRule.getRandomSpawnCell(board);
        currentCell.setMysteryCell(true);
        roundsActive = 0;
    }

    // resolves a landing on the mystery cell into a TeleportDestination (Rule T-11)
    public Optional<TeleportDestination> handleLanding(Piece piece) {
        if (currentCell == null) {
            return Optional.empty();
        }
        return Optional.of(mysteryRule.resolveTeleport(piece, board));
    }

    public boolean isActive() {
        return currentCell != null;
    }

    // the caller then spawns it again at another place (T-10: it reappears elsewhere, never twice in a row on the same cell)
    public Optional<Cell> tick() {
        if (currentCell == null) {
            return Optional.empty();
        }
        roundsActive++;
        if (roundsActive < LIFESPAN) {
            return Optional.empty();
        }
        Cell expired = currentCell;
        expired.setMysteryCell(false);
        currentCell = null;
        roundsActive = 0;
        return Optional.of(expired);
    }

    public Cell getCurrentCell() {
        return currentCell;
    }

    public int getRoundsLeft() {
        return Math.max(0, LIFESPAN - roundsActive);
    }

    public int getLifespan() {
        return LIFESPAN;
    }
}
