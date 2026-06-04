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

public class RuleEngine {

    private final BaseMoveValidator moveValidator;
    private final BaseCaptureRule captureRule;
    private final BlockRule blockRule;
    private final BaseHomeEntryRule homeEntryRule;
    private final BaseDiceRule diceRule;
    private final MysteryRule mysteryRule;
    private final EffectRule effectRule;

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
        Direction dir = moveValidator.assignDirection(mfb.getPiece());
        mfb.getPiece().setDirection(dir);
        Cell startCell = Board.getInstance().getStartCell(mfb.getPiece().getColour());
        if (blockRule != null && blockRule.isBlocked(mfb.getPiece(), startCell)) {
            return;
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

        Optional<Cell> destOpt = moveValidator.getDestinationCell(p, mob.getSteps());
        if (destOpt.isEmpty()) {
            return;
        }
        Cell dest = destOpt.get();
        if (blockRule != null && blockRule.isBlocked(p, dest)) {
            int partialSteps = stepsBeforeBlock(p, mob.getSteps());
            if (partialSteps > 0) {
                valid.add(new MoveOnBoard(p, partialSteps));
            }
            return;
        }
        Piece opponent = singleOpponent(dest, p);
        if (opponent != null) {
            valid.add(new CaptureMove(p, opponent, dest, mob.getSteps()));
        } else {
            valid.add(mob);
        }
    }

    private void addBlockMove(Cell currentCell, Set<Cell> processedBlocks, List<Move> valid, int roll) {
        processedBlocks.add(currentCell);
        List<Piece> block = new ArrayList<>(currentCell.getPieces());
        int blockSteps = blockRule.calcBlockSteps(roll, block.size());
        if (blockSteps == 0) {
            return;
        }
        Cell target = blockDestination(currentCell, blockRule.calcBlockMoveDir(block), blockSteps);
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
            if (blockRule.isBlocked(piece, Board.getInstance().getCell(testId))) {
                return s - 1;
            }
        }
        return maxSteps - 1;
    }

    // calculates the target cell for a block moving in its direction by the given steps
    private Cell blockDestination(Cell source, Direction dir, int steps) {
        int id = source.getId();
        int newId = dir == Direction.CLOCKWISE
                ? (id + steps) % GameConstants.BOARD_SIZE
                : ((id - steps) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        return Board.getInstance().getCell(newId);
    }

    public void applyLandingEffects(Piece piece) {
        if (homeEntryRule.canEnterHomeStraight(piece)) {
            List<Cell> hs = Board.getInstance().getHomeStraight(piece.getColour());
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
        Board board = Board.getInstance();
        switch (dest) {
            case ALPHA -> {
                if (effectRule != null) {
                    effectRule.applyAlpha(piece);
                }
            }
            case BETA -> {
                if (effectRule != null) {
                    effectRule.applyBeta(piece);
                }
            }
            case GAMMA -> {
                Direction dirBefore = piece.getDirection();
                if (effectRule != null) {
                    effectRule.applyGamma(piece);
                }
                if (dirBefore == Direction.CLOCKWISE && effectRule != null) {
                    effectRule.applyBeta(piece);
                }
            }
            case BASE -> piece.sendToBase();
            case START_CELL -> teleportTo(piece, board.getStartCell(piece.getColour()));
            case APPROACH_CELL -> teleportTo(piece, board.getApproachCell(piece.getColour()));
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
