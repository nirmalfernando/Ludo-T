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
import com.ludot.model.state.OnBoardState;
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
        piece.setDirection(moveValidator.assignDirection(piece));
        Cell startCell = piece.getBoard().getStartCell(piece.getColour());
        if (isBlocked(piece, startCell)) {
            publishBlocked(piece, null, startCell);
            return;
        }
        if (landsOnOwnPiece(piece, startCell)) {
            return; // Rule 7 (Classic): a move onto the player's own piece is not permitted
        }
        valid.add(mfb);
    }

    private void addMoveOnBoard(MoveOnBoard mob, Set<Cell> processedBlocks, List<Move> valid, int roll) {
        Piece p = mob.getPiece();
        Cell currentCell = p.getPosition();

        if (blockRule != null && currentCell != null && currentCell.hasBlock()) {
            if (!processedBlocks.contains(currentCell)) {
                addBlockMove(currentCell, processedBlocks, valid, roll);
            }
            return;
        }
        moveValidator.getDestinationCell(p, mob.getSteps())
                .ifPresent(destination -> addMoveTo(destination, mob, valid));
    }

    // a single piece's move to a known destination: stopped short by a blockade, a capture, or a plain move
    private void addMoveTo(Cell dest, MoveOnBoard mob, List<Move> valid) {
        Piece p = mob.getPiece();
        if (landsOnOwnPiece(p, dest)) {
            return; // Rule 7 (Classic): a move onto the player's own piece is not permitted
        }
        if (isBlocked(p, dest)) {
            publishBlocked(p, p.getPosition(), dest);
            int partialSteps = stepsBeforeBlock(p, mob.getSteps());
            if (partialSteps > 0) {
                valid.add(new MoveOnBoard(p, partialSteps));
            }
            return;
        }
        Piece opponent = singleOpponent(dest, p);
        valid.add(opponent != null ? new CaptureMove(p, opponent, dest, mob.getSteps()) : mob);
    }

    // Rule 7 applies on the standard path in Classic, where there are no blocks; LUDO-T replaces it with T-3
    private boolean landsOnOwnPiece(Piece piece, Cell dest) {
        return blockRule == null && dest.getId() < GameConstants.BOARD_SIZE
                && dest.getPieces().stream().anyMatch(other -> other != piece && other.getColour() == piece.getColour());
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

    private void addBlockMove(Cell currentCell, Set<Cell> processedBlocks, List<Move> valid, int roll) {
        processedBlocks.add(currentCell);
        List<Piece> block = new ArrayList<>(currentCell.getPieces());
        int blockSteps = blockRule.calcBlockSteps(roll, block.size());
        if (blockSteps == 0) {
            return;
        }
        Cell target = blockDestination(block.get(0).getBoard(), currentCell, blockRule.calcBlockMoveDir(block), blockSteps);
        if (blockRule.canCaptureBlock(block, target)) {
            valid.add(new BlockCaptureMove(block, new ArrayList<>(target.getPieces()), target, blockSteps));
        } else if (!blockRule.isBlocked(block.get(0), target)) {
            valid.add(new BlockMove(block, target, blockSteps, roll));
        }
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

    // walks the path one step at a time to find how many steps place the piece on the cell
    // immediately before the first blockade encountered, up to maxSteps.
    private int stepsBeforeBlock(Piece piece, int maxSteps) {
        int id = piece.getPosition().getId();
        for (int s = 1; s < maxSteps; s++) {
            int testId = piece.getDirection() == Direction.CLOCKWISE
                    ? (id + s) % GameConstants.BOARD_SIZE
                    : ((id - s) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
            if (blockRule.isBlocked(piece, piece.getBoard().getCell(testId))) {
                return s - 1;
            }
        }
        return maxSteps - 1;
    }

    // calculates the target cell for a block moving in its direction by the given steps
    private Cell blockDestination(Board board, Cell source, Direction dir, int steps) {
        int id = source.getId();
        int newId = dir == Direction.CLOCKWISE
                ? (id + steps) % GameConstants.BOARD_SIZE
                : ((id - steps) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        return board.getCell(newId);
    }

    public void applyLandingEffects(Piece piece) {
        if (homeEntryRule.canEnterHomeStraight(piece)) {
            List<Cell> hs = piece.getBoard().getHomeStraight(piece.getColour());
            Cell first = hs.get(0);
            piece.getPosition().removePiece(piece);
            first.addPiece(piece);
            piece.setPosition(first);
            if (piece.getState() instanceof OnBoardState obs) {
                obs.transitionToHomeStraight(piece);
            }
        }
    }

    public void applyMysteryEffect(Piece piece, TeleportDestination dest) {
        applyTeleport(piece, dest);
    }

    private void applyTeleport(Piece piece, TeleportDestination dest) {
        Board board = piece.getBoard();
        switch (dest) {
            case ALPHA -> withEffectRule(rule -> rule.applyAlpha(piece));
            case BETA -> withEffectRule(rule -> rule.applyBeta(piece));
            case GAMMA -> withEffectRule(rule -> applyGamma(rule, piece));
            case BASE -> piece.sendToBase();
            case START_CELL -> teleportTo(piece, board.getStartCell(piece.getColour()));
            case APPROACH_CELL -> teleportTo(piece, board.getApproachCell(piece.getColour()));
        }
    }

    // T-14: Gamma reverses direction; a piece that was going clockwise is then also sent to Briefing (Beta)
    private static void applyGamma(EffectRule rule, Piece piece) {
        Direction dirBefore = piece.getDirection();
        rule.applyGamma(piece);
        if (dirBefore == Direction.CLOCKWISE) {
            rule.applyBeta(piece);
        }
    }

    private void withEffectRule(Consumer<EffectRule> action) {
        if (effectRule != null) {
            action.accept(effectRule);
        }
    }

    private void teleportTo(Piece piece, Cell target) {
        if (piece.getPosition() != null) {
            piece.getPosition().removePiece(piece);
        }
        target.addPiece(piece);
        piece.setPosition(target);
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
