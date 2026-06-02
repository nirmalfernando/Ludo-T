package com.ludot.rules;

import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.AtBriefingState;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.InHomeStraightState;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Mediator that coordinates all rule objects and exposes a unified query API to the game engine.
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
        // tracks block source cells already handled to avoid duplicate BlockMoves
        Set<Cell> processedBlocks = new HashSet<>();

        for (Piece piece : player.getPieces()) {
            List<Move> candidates = piece.getState().handleRoll(piece, roll);

            for (Move candidate : candidates) {
                if (candidate instanceof MoveFromBase mfb) {
                    Direction dir = moveValidator.assignDirection(mfb.getPiece());
                    mfb.getPiece().setDirection(dir);
                    Cell startCell = Board.getInstance().getStartCell(piece.getColour());
                    if (blockRule.isBlocked(piece, startCell)) continue;
                    Piece opponent = singleOpponent(startCell, piece);
                    if (opponent != null) {
                        valid.add(new CaptureMove(piece, opponent, startCell, 0));
                    } else {
                        valid.add(mfb);
                    }

                } else if (candidate instanceof MoveOnBoard mob) {
                    Piece p = mob.getPiece();
                    Cell currentCell = p.getPosition();

                    // if piece is part of a block, the whole block moves as one unit (T-5)
                    if (currentCell != null && currentCell.hasBlock()
                            && !processedBlocks.contains(currentCell)) {
                        processedBlocks.add(currentCell);
                        List<Piece> block = new ArrayList<>(currentCell.getPieces());
                        int blockSteps = blockRule.calcBlockSteps(roll, block.size());
                        if (blockSteps > 0) {
                            Cell target = blockDestination(currentCell,
                                    blockRule.calcBlockMoveDir(block), blockSteps);
                            if (!blockRule.isBlocked(block.get(0), target)) {
                                valid.add(new BlockMove(block, target, blockSteps));
                            }
                        }
                        continue;
                    }

                    // skip if piece is already in a block processed via another piece
                    if (currentCell != null && currentCell.hasBlock()
                            && processedBlocks.contains(currentCell)) {
                        continue;
                    }

                    Cell dest = moveValidator.getDestinationCell(p, mob.getSteps());
                    if (dest == null) continue;
                    if (blockRule.isBlocked(p, dest)) continue;
                    Piece opponent = singleOpponent(dest, p);
                    if (opponent != null) {
                        valid.add(new CaptureMove(p, opponent, dest, mob.getSteps()));
                    } else {
                        valid.add(mob);
                    }
                }
            }
        }

        if (valid.isEmpty()) {
            valid.add(new SkipMove(player));
        }
        return valid;
    }

    // returns the single opponent on a cell, or null if cell is empty/has a block/has friendlies
    private Piece singleOpponent(Cell cell, Piece attacker) {
        if (!cell.isOccupied() || cell.hasBlock()) return null;
        List<Piece> occupants = cell.getPieces();
        if (occupants.size() == 1 && occupants.get(0).getColour() != attacker.getColour()) {
            return occupants.get(0);
        }
        return null;
    }

    // calculates the target cell for a block moving in its direction by the given steps
    private Cell blockDestination(Cell source, Direction dir, int steps) {
        int id = source.getId();
        int newId = dir == Direction.CLOCKWISE
                ? (id + steps) % 52
                : ((id - steps) % 52 + 52) % 52;
        return Board.getInstance().getCell(newId);
    }

    // home-straight entry only — mystery is handled separately by MysteryManager + applyMysteryEffect
    public RuleResult applyLandingEffects(Piece piece, Cell cell) {
        if (homeEntryRule.canEnterHomeStraight(piece)) {
            List<Cell> hs = Board.getInstance().getHomeStraight(piece.getColour());
            Cell first = hs.get(0);
            piece.getPosition().removePiece(piece);
            first.addPiece(piece);
            piece.setPosition(first);
            piece.setState(new InHomeStraightState());
        }
        return RuleResult.valid();
    }

    // called by MoveExecutor after MysteryManager resolves the destination
    public void applyMysteryEffect(Piece piece, TeleportDestination dest) {
        applyTeleport(piece, dest);
    }

    private void applyTeleport(Piece piece, TeleportDestination dest) {
        Board board = Board.getInstance();
        switch (dest) {
            case ALPHA -> effectRule.applyAlpha(piece);
            case BETA  -> effectRule.applyBeta(piece);
            case GAMMA -> {
                effectRule.applyGamma(piece);
                // T-14: CW→CCW flip also sends piece to briefing room
                if (piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
                    effectRule.applyBeta(piece);
                }
            }
            case BASE         -> piece.sendToBase();
            case START_CELL   -> teleportTo(piece, board.getStartCell(piece.getColour()));
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

    public int calculateEffectiveSteps(Piece piece, int roll) {
        return piece.getEffectiveSteps(roll);
    }

    public BaseMoveValidator getMoveValidator() { return moveValidator; }
    public BaseCaptureRule getCaptureRule() { return captureRule; }
    public BlockRule getBlockRule() { return blockRule; }
    public BaseHomeEntryRule getHomeEntryRule() { return homeEntryRule; }
    public BaseDiceRule getDiceRule() { return diceRule; }
    public MysteryRule getMysteryRule() { return mysteryRule; }
    public EffectRule getEffectRule() { return effectRule; }
}
