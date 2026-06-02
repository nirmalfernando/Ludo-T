package com.ludot.rules.mystery;

import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import java.util.HashSet;
import java.util.Set;

// T-10/11/15: controls mystery cell spawn timing, location, and teleport outcome.
public class MysteryRule {

    private final RandomProvider random;
    private final Set<Integer> previousLocations = new HashSet<>();

    public MysteryRule(RandomProvider random) {
        this.random = random;
    }

    // T-10: mystery cell only appears after round 2
    public boolean shouldSpawn(int roundCount) {
        return roundCount > 2;
    }

    // T-10: no consecutive repeat of the same cell location
    public Cell getRandomSpawnCell(Board board) {
        int cellId;
        do {
            cellId = random.getRandomCell(52);
        } while (previousLocations.contains(cellId));
        previousLocations.clear();
        previousLocations.add(cellId);
        return board.getCell(cellId);
    }

    // T-11: roll 1–6 to determine the teleport destination
    public TeleportDestination resolveTeleport(Piece piece, Board board) {
        return switch (random.getMysteryEffect()) {
            case 1 -> TeleportDestination.ALPHA;
            case 2 -> TeleportDestination.BETA;
            case 3 -> TeleportDestination.GAMMA;
            case 4 -> TeleportDestination.BASE;
            case 5 -> TeleportDestination.START_CELL;
            default -> TeleportDestination.APPROACH_CELL;
        };
    }
}
