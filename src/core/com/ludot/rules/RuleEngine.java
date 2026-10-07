package com.ludot.rules;

import com.ludot.GameConstants;
import com.ludot.command.BlockCaptureMove;
import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.effects.TeleportDestination;
import com.ludot.events.EventBus;
import com.ludot.events.move.PieceBlockedEvent;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.block.BlockRule;
import com.ludot.rules.capture.BaseCaptureRule;
import com.ludot.rules.dice.BaseDiceRule;
import com.ludot.rules.home.BaseHomeEntryRule;
import com.ludot.rules.move.BaseMoveValidator;
import com.ludot.rules.move.PiecePath;
import com.ludot.rules.mystery.EffectRule;
import com.ludot.rules.mystery.MysteryRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RuleEngine {

    private final BaseMoveValidator moveValidator;
    private final BaseCaptureRule captureRule;
    private final BlockRule blockRule;
    private final BaseHomeEntryRule homeEntryRule;
    private final BaseDiceRule diceRule;
    private final MysteryRule mysteryRule;
    private final EffectRule effectRule;
    private EventBus eventBus;

    public RuleEngine(BaseMoveValidator moveValidator, BaseCaptureRule captureRule,
                      BlockRule blockRule, BaseHomeEntryRule homeEntryRule,
                      BaseDiceRule diceRule, MysteryRule mysteryRule, EffectRule effectRule) {
        this.moveValidator = moveValidator;
        this.captureRule = captureRule;
        this.blockRule = blockRule;
        this.homeEntryRule = homeEntryRule;
        this.diceRule = diceRule;
        this.mysteryRule = mysteryRule;
        this.effectRule = effectRule;
    }

    /** True when this rule set has blockades (LUDO-T); Classic does not. */
    public boolean supportsBlocks() {
        return blockRule != null;
    }

    /** True when this rule set has mystery cells (LUDO-T); Classic does not. */
    public boolean supportsMysteryCells() {
        return mysteryRule != null;
    }

    public void setEventBus(EventBus bus) {
        this.eventBus = bus;
    }

    public List<Move> getValidMoves(Player player, int roll) {
        List<Move> valid = new ArrayList<>();
        Set<Cell> processedBlocks = new HashSet<>();

        for (Piece piece : player.getPieces()) {
            for (Move candidate : piece.getState().handleRoll(piece, roll)) {
                if (candidate instanceof MoveFromBase mfb) {
                    addMoveFromBase(mfb, valid);
                } else if (candidate instanceof MoveOnBoard mob) {
                    addMoveOnBoard(mob, processedBlocks, valid, roll);
                }
            }
        }

        if (valid.isEmpty()) {
            valid.add(new SkipMove(player));
        }
        return valid;
    }

    private void addMoveFromBase(MoveFromBase mfb, List<Move> valid) {
        Piece piece = mfb.getPiece();
        Cell startCell = piece.getBoard().getStartCell(piece.getColour());
        if (isBlocked(piece, startCell)) {
            publishBlocked(piece, null, startCell);
            return;
        }
        if (landsOnOwnPiece(piece, startCell)) {
            return; // Rule 7 (Classic): a move onto the player's own piece is not permitted
        }
        Piece lone = singleOpponent(startCell, piece);
        valid.add(lone != null ? new MoveFromBase(piece, lone) : mfb); // Rule 6: arriving on a lone opponent captures it
    }

    private void addMoveOnBoard(MoveOnBoard mob, Set<Cell> processedBlocks, List<Move> valid, int roll) {
        Piece p = mob.getPiece();
        Cell currentCell = p.getPosition();

        if (blockRule != null && currentCell != null && isOnPath(currentCell) && currentCell.hasBlock()) {
            if (!processedBlocks.contains(currentCell)) {
                addBlockMove(currentCell, processedBlocks, valid, roll);
            }
            // T-5: a piece may also leave its block on its own, travelling in its original direction again
            planMove(p, mob.getSteps(), directionAfterLeavingBlock(p)).ifPresent(valid::add);
            return;
        }
        planMove(p, mob.getSteps()).ifPresent(valid::add);
    }

    /** The direction a piece travels in once it is out of a block: the one it was given when it was placed on X (T-5). */
    public static Direction directionAfterLeavingBlock(Piece piece) {
        return piece.getOriginalDirection() != null ? piece.getOriginalDirection() : piece.getDirection();
    }

    /**
     * Plans one piece's move of the given steps along its real route (Rule 9: into the home straight when it lands on
     * or passes the approach cell, if allowed). A blockade anywhere on the way stops the piece on the cell before it
     * (T-3); a lone opponent on the last cell is captured (Rule 6); landing on an own piece is refused in Classic
     * (Rule 7). Empty if nothing is possible, for example an overshoot of home (Rule 10).
     */
    public Optional<Move> planMove(Piece piece, int steps) {
        return planMove(piece, steps, piece.getDirection());
    }

    /** The same, travelling in the given direction. */
    public Optional<Move> planMove(Piece piece, int steps, Direction direction) {
        List<Cell> route = PiecePath.route(piece, steps, homeEntryRule, direction);
        if (route.isEmpty()) {
            return Optional.empty();
        }
        int blockedAt = firstBlockedIndex(piece, route);
        List<Cell> travelled = route;
        if (blockedAt >= 0) {
            publishBlocked(piece, piece.getPosition(), route.get(blockedAt));
            travelled = new ArrayList<>(route.subList(0, blockedAt));
        }
        if (travelled.isEmpty()) {
            return Optional.empty();
        }
        Cell dest = travelled.get(travelled.size() - 1);
        if (landsOnOwnPiece(piece, dest)) {
            return Optional.empty(); // Rule 7 (Classic): a move onto the player's own piece is not permitted
        }
        Piece opponent = singleOpponent(dest, piece);
        return Optional.of(opponent != null
                ? new CaptureMove(piece, opponent, dest, travelled.size(), travelled)
                : new MoveOnBoard(piece, travelled.size(), travelled));
    }

    private int firstBlockedIndex(Piece piece, List<Cell> route) {
        for (int i = 0; i < route.size(); i++) {
            if (isBlocked(piece, route.get(i))) {
                return i;
            }
        }
        return -1;
    }

    // Rule 7 applies on the standard path in Classic, where there are no blocks; LUDO-T replaces it with T-3
    private boolean landsOnOwnPiece(Piece piece, Cell dest) {
        return blockRule == null && isOnPath(dest)
                && dest.getPieces().stream().anyMatch(other -> other != piece && other.getColour() == piece.getColour());
    }

    // the 52 standard cells, as opposed to the home straights (whose cell ids come after them)
    private static boolean isOnPath(Cell cell) {
        return cell.getId() < GameConstants.BOARD_SIZE;
    }

    // the blockade rule and the event bus are optional collaborators, so both are checked in one place each
    private boolean isBlocked(Piece piece, Cell cell) {
        return blockRule != null && blockRule.isBlocked(piece, cell);
    }

    private void publishBlocked(Piece piece, Cell from, Cell intendedDest) {
        if (eventBus != null) {
            eventBus.publish(new PieceBlockedEvent(piece, from, intendedDest, List.copyOf(intendedDest.getPieces())));
        }
    }

    /**
     * T-4: a block moves as one, roll / size cells in the direction of the member furthest from home. Like single
     * pieces it cannot jump an opposing block (T-3); it captures an opposing block of the same size (T-8) and a
     * lone opponent on its last cell (Rule 6). Blocks stay on the 52-cell path: a piece must leave the block to
     * turn into its home straight.
     */
    private void addBlockMove(Cell currentCell, Set<Cell> processedBlocks, List<Move> valid, int roll) {
        processedBlocks.add(currentCell);
        List<Piece> block = new ArrayList<>(currentCell.getPieces());
        int blockSteps = blockRule.calcBlockSteps(roll, block.size());
        if (blockSteps == 0) {
            return;
        }
        Board board = block.get(0).getBoard();
        Direction direction = blockRule.calcBlockMoveDir(block);
        List<Cell> route = pathCells(board, currentCell, direction, blockSteps);
        int blockedAt = firstBlockedIndex(block, route);
        List<Cell> travelled = blockedAt >= 0 ? route.subList(0, blockedAt) : route;
        if (travelled.isEmpty()) {
            return;
        }
        Cell target = travelled.get(travelled.size() - 1);
        int steps = travelled.size();
        Piece lone = singleOpponent(target, block.get(0));
        if (blockRule.canCaptureBlock(block, target)) {
            valid.add(new BlockCaptureMove(block, new ArrayList<>(target.getPieces()), target, steps));
        } else if (lone != null) {
            valid.add(new BlockCaptureMove(block, List.of(lone), target, steps));
        } else {
            valid.add(new BlockMove(block, target, steps, roll));
        }
    }

    // the cells a block passes on the standard path; the last cell may hold an opposing block of its own size
    // (a capture, T-8) which is not an obstacle, every other opposing block is
    private int firstBlockedIndex(List<Piece> block, List<Cell> route) {
        for (int i = 0; i < route.size(); i++) {
            Cell cell = route.get(i);
            boolean last = i == route.size() - 1;
            if (blockRule.isBlocked(block.get(0), cell) && !(last && blockRule.canCaptureBlock(block, cell))) {
                return i;
            }
        }
        return -1;
    }

    private static List<Cell> pathCells(Board board, Cell from, Direction direction, int steps) {
        List<Cell> cells = new ArrayList<>(steps);
        Cell at = from;
        for (int step = 0; step < steps; step++) {
            at = PiecePath.neighbour(board, at, direction);
            cells.add(at);
        }
        return cells;
    }

    // returns the single opponent on a cell, or null if cell is empty/has a block/has friendlies
    private Piece singleOpponent(Cell cell, Piece attacker) {
        if (!cell.isOccupied() || cell.hasBlock()) {
            return null;
        }
        List<Piece> occupants = cell.getPieces();
        if (occupants.size() == 1 && occupants.get(0).getColour() != attacker.getColour()) {
            return occupants.get(0);
        }
        return null;
    }

    /**
     * T-11 to T-15: teleports a piece that has landed on the mystery cell. Alpha, Beta and Gamma are real cells on the
     * path (7, 25 and 44) and their effects apply only through this teleport (T-15). A lone opponent on the destination
     * is captured; an opposing block on it refuses the teleport and the piece stays where it is.
     *
     * @return the opponent piece captured by arriving, if any
     */
    public Optional<Piece> applyMysteryEffect(Piece piece, TeleportDestination dest) {
        Board board = piece.getBoard();
        return switch (dest) {
            case ALPHA -> arriveAt(piece, board.getCell(GameConstants.ALPHA_CELL_ID), rule -> rule.applyAlpha(piece));
            case BETA -> arriveAt(piece, board.getCell(GameConstants.BETA_CELL_ID), rule -> rule.applyBeta(piece));
            case GAMMA -> gamma(piece, board);
            case BASE -> {
                piece.sendToBase();
                yield Optional.empty();
            }
            case START_CELL -> arriveAt(piece, board.getStartCell(piece.getColour()), rule -> { });
            case APPROACH_CELL -> arriveAt(piece, board.getApproachCell(piece.getColour()), rule -> { });
        };
    }

    // T-14: clockwise, the piece changes to counter-clockwise; counter-clockwise, it is teleported on to Beta
    private Optional<Piece> gamma(Piece piece, Board board) {
        if (piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
            return arriveAt(piece, board.getCell(GameConstants.BETA_CELL_ID), rule -> rule.applyBeta(piece));
        }
        return arriveAt(piece, board.getCell(GameConstants.GAMMA_CELL_ID), rule -> rule.applyGamma(piece));
    }

    private Optional<Piece> arriveAt(Piece piece, Cell target, Consumer<EffectRule> effect) {
        if (isBlocked(piece, target)) {
            return Optional.empty();
        }
        Piece victim = singleOpponent(target, piece);
        if (piece.getPosition() != null) {
            piece.getPosition().removePiece(piece);
        }
        target.addPiece(piece);
        piece.setPosition(target);
        if (victim != null) {
            captureRule.capture(piece, victim);
        }
        if (effectRule != null) {
            effect.accept(effectRule);
        }
        return Optional.ofNullable(victim);
    }

    public BaseMoveValidator getMoveValidator() {
        return moveValidator;
    }

    public BaseCaptureRule getCaptureRule() {
        return captureRule;
    }

    public BlockRule getBlockRule() {
        return blockRule;
    }

    public BaseHomeEntryRule getHomeEntryRule() {
        return homeEntryRule;
    }

    public BaseDiceRule getDiceRule() {
        return diceRule;
    }

    public MysteryRule getMysteryRule() {
        return mysteryRule;
    }

    public EffectRule getEffectRule() {
        return effectRule;
    }
}
