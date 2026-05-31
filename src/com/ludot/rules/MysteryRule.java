package com.ludot.rules;

import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;

// Encapsulates mystery-cell spawn, expiry, and teleport outcome rules (T-10/11/15).

public class MysteryRule {

    private final RandomProvider random;

    public MysteryRule(RandomProvider random) {
        this.random = random;
    }

    public boolean shouldSpawn(int roundCount) {
        return false;
    }

    public Cell getRandomSpawnCell(Board board) {
        return null;
    }

    public TeleportDestination resolveTeleport(Piece piece, Board board) {
        return null;
    }
}
