package com.ludot.rules.mystery;

import com.ludot.GameConstants;
import com.ludot.dice.RandomProvider;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.rules.GameRule;
import com.ludot.rules.MoveContext;
import com.ludot.rules.RuleResult;
import java.util.HashSet;
import java.util.Set;

public class MysteryRule implements GameRule {

    private final RandomProvider random;
    private final Set<Integer> previousLocations = new HashSet<>();

    
    public MysteryRule(RandomProvider random) {
        this.random = random;
    }

    
    public boolean shouldSpawn(int roundCount) {
        return roundCount > 2;
    }

    
    public Cell getRandomSpawnCell(Board board) {
        int cellId;
        do {
            cellId = random.getRandomCell(GameConstants.BOARD_SIZE);
        } while (previousLocations.contains(cellId) || board.getCell(cellId).isOccupied());
        previousLocations.clear();
        previousLocations.add(cellId);
        return board.getCell(cellId);
    }

    
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

    @Override
    public boolean isApplicable(MoveContext ctx) {
        Cell dest = ctx.destination();
        return dest != null && dest.isMysteryCell();
    }

    @Override
    public RuleResult apply(MoveContext ctx) {
        return isApplicable(ctx) ? RuleResult.success("Mystery cell effect will be triggered")
                                 : RuleResult.notApplicable();
    }

    @Override
    public int getPriority() { return 70; }
}
