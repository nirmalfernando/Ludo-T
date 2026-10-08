package com.ludot.engine;

import com.ludot.GameConstants;
import com.ludot.command.BlockCaptureMove;
import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.MoveResult;
import com.ludot.command.SkipMove;
import com.ludot.effects.TeleportDestination;
import com.ludot.events.OutputFormatter;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.block.BlockMoveEvent;
import com.ludot.events.move.ApproachPassEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.EventBus;
import com.ludot.events.home.HomeEntryEvent;
import com.ludot.events.home.HomeEntryRejectedEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.events.mystery.MysteryTeleportEvent;
import com.ludot.model.state.AtHomeState;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.Piece;
import com.ludot.model.state.PieceState;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import com.ludot.events.block.BlockBreakEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoveExecutor {

    private final Board board;
    private final RuleEngine ruleEngine;
    private final MysteryManager mysteryManager;
    private final EventBus eventBus;
    private final List<Player> players;

    public MoveExecutor(Board board, RuleEngine ruleEngine,
                        MysteryManager mysteryManager, EventBus eventBus) {
        this(board, ruleEngine, mysteryManager, eventBus, List.of());
    }

    public MoveExecutor(Board board, RuleEngine ruleEngine,
                        MysteryManager mysteryManager, EventBus eventBus, List<Player> players) {
        this.board = board;
        this.ruleEngine = ruleEngine;
        this.mysteryManager = mysteryManager;
        this.eventBus = eventBus;
        this.players = players;
    }

    public MoveResult execute(Move move) {
        return execute(move, null);
    }

    public MoveResult execute(Move move, Player player) {
        if (move instanceof MoveFromBase mfb) {
            return executeFromBase(mfb, player);
        }
        if (move instanceof CaptureMove cm) {
            return executeCapture(cm, player);
        }
        if (move instanceof BlockCaptureMove bcm) {
            return executeBlockCapture(bcm, player);
        }
        if (move instanceof BlockMove bm) {
            return executeBlock(bm);
        }
        if (move instanceof MoveOnBoard mob) {
            return executeOnBoard(mob, player);
        }
        if (move instanceof SkipMove) {
            return executeSkip(move);
        }
        return move.execute();
    }

    private MoveResult executeFromBase(MoveFromBase mfb, Player player) {
        Piece piece = mfb.getPiece();
        piece.setDirection(ruleEngine.getMoveValidator().assignDirection(piece)); // T-1: the coin toss, on X
        if (ruleEngine.getMoveValidator().usesCoinToss()) {
            eventBus.publish(new CoinTossEvent(piece, piece.getDirection()));
        }
        piece.setOriginalDirection(piece.getDirection()); // T-5: the direction it is given when placed on X
        MoveResult result = mfb.execute();
        Cell dest = piece.getPosition();
        Piece victim = mfb.getVictim();
        if (victim != null) {
            ruleEngine.getCaptureRule().capture(piece, victim); // Rule 6 and T-9
            Player victimPlayer = findPlayer(victim.getColour()).orElse(null);
            eventBus.publish(new CaptureEvent(piece, victim, dest, victimPlayer));
        }
        eventBus.publish(new MoveEvent(player, piece, null, dest, 0));
        checkBlockForm(piece, dest);
        applyLanding(piece, dest, player, null);
        return result;
    }

    private MoveResult executeOnBoard(MoveOnBoard mob, Player player) {
        Piece piece = mob.getPiece();
        Cell from = piece.getPosition();
        boolean leavesBlock = isInBlock(from);
        if (leavesBlock) {
            piece.setDirection(RuleEngine.directionAfterLeavingBlock(piece)); // T-5
        }
        int passCountBefore = piece.getApproachPassCount();
        PieceState stateBefore = piece.getState();
        MoveResult result = mob.execute();
        if (leavesBlock) {
            announceBlockBroken(from, piece);
        }
        Cell dest = piece.getPosition();
        if (piece.getApproachPassCount() > passCountBefore) {
            eventBus.publish(new ApproachPassEvent(piece, piece.getApproachPassCount()));
        }
        eventBus.publish(new MoveEvent(player, piece, from, dest, mob.getSteps()));
        if (piece.getState() instanceof AtHomeState) {
            eventBus.publish(new HomeReachEvent(piece));
            return result;
        }
        checkBlockForm(piece, dest);
        applyLanding(piece, dest, player, mob.getRoute());
        if (!(stateBefore instanceof InHomeStraightState)
                && piece.getState() instanceof InHomeStraightState) {
            eventBus.publish(new HomeEntryEvent(piece));
        }
        return result;
    }

    private MoveResult executeCapture(CaptureMove cm, Player player) {
        Piece attacker = cm.getAttacker();
        Cell from = attacker.getPosition();
        Cell targetCell = cm.getTargetCell();
        boolean leavesBlock = isInBlock(from);
        if (leavesBlock) {
            attacker.setDirection(RuleEngine.directionAfterLeavingBlock(attacker)); // T-5
        }
        MoveResult cmdResult = cm.execute(); // moves attacker to target cell
        if (leavesBlock) {
            announceBlockBroken(from, attacker);
        }
        Piece victim = cmdResult.getCaptured();
        ruleEngine.getCaptureRule().capture(attacker, victim); // victim reset + T-9 + capture count
        Player victimPlayer = findPlayer(victim.getColour()).orElse(null);
        eventBus.publish(new CaptureEvent(attacker, victim, targetCell, victimPlayer));
        eventBus.publish(new MoveEvent(player, attacker, from, attacker.getPosition(), cm.getSteps()));
        MoveResult result = new MoveResult(true, victim, false, true,
                attacker.getName() + " captures " + victim.getName());
        applyLanding(attacker, attacker.getPosition(), player, cm.getRoute());
        return result;
    }

    private MoveResult executeBlock(BlockMove bm) {
        List<Piece> block = bm.getBlock();
        Cell from = block.get(0).getPosition();
        Direction direction = ruleEngine.getBlockRule().calcBlockMoveDir(block);
        MoveResult result = bm.execute();
        block.forEach(piece -> piece.setDirection(direction)); // T-4: the whole block travels the way its furthest member does
        Cell dest = block.get(0).getPosition();
        eventBus.publish(new BlockMoveEvent(block, from, dest, bm.getSteps(), bm.getOriginalRoll()));
        return result;
    }

    // T-8: block-vs-block capture — attacker movement done by the command
    private MoveResult executeBlockCapture(BlockCaptureMove bcm, Player player) {
        Cell from = bcm.getAttackers().get(0).getPosition();
        Direction direction = ruleEngine.getBlockRule().calcBlockMoveDir(bcm.getAttackers());
        bcm.execute(); // moves all attackers
        bcm.getAttackers().forEach(piece -> piece.setDirection(direction)); // T-4
        Cell dest = bcm.getTargetCell();
        // apply capture rule to each victim, paired with the corresponding attacker
        List<Piece> attackers = bcm.getAttackers();
        List<Piece> victims   = bcm.getVictims();
        for (int i = 0; i < victims.size(); i++) {
            Piece attacker = attackers.get(i % attackers.size());
            Piece victim   = victims.get(i);
            ruleEngine.getCaptureRule().capture(attacker, victim);
            Player victimPlayer = findPlayer(victim.getColour()).orElse(null);
            eventBus.publish(new CaptureEvent(attacker, victim, dest, victimPlayer));
        }
        eventBus.publish(new BlockMoveEvent(attackers, from, dest, bcm.getSteps(), 0));
        return new MoveResult(true, null, false, true,
                "Block of " + attackers.size() + " captures opposing block");
    }

    private MoveResult executeSkip(Move skip) {
        return skip.execute();
    }

    // a counter-clockwise piece sent to Gamma ends up on Beta instead (T-14)
    private static String actualDestination(TeleportDestination dest, Direction dirBefore) {
        boolean sentOnToBeta = dest == TeleportDestination.GAMMA && dirBefore == Direction.COUNTER_CLOCKWISE;
        return sentOnToBeta ? TeleportDestination.BETA.name() : dest.name();
    }

    private boolean isInBlock(Cell cell) {
        return ruleEngine.supportsBlocks() && cell != null && cell.getId() < GameConstants.BOARD_SIZE && cell.hasBlock();
    }

    // T-5: the piece that left has its original direction back; a lone piece left behind is no longer in a block either
    private void announceBlockBroken(Cell from, Piece leaver) {
        List<Piece> affected = new ArrayList<>(List.of(leaver));
        if (from.getPieces().size() == 1) {
            Piece left = from.getPieces().get(0);
            left.setDirection(RuleEngine.directionAfterLeavingBlock(left));
            affected.add(left);
        }
        eventBus.publish(new BlockBreakEvent(affected, from));
    }

    private void checkBlockForm(Piece piece, Cell dest) {
        if (dest != null && dest.getId() < GameConstants.BOARD_SIZE && dest.hasBlock() && dest.getPieces().contains(piece)
                && dest.getBlockSize() >= 2) {
            eventBus.publish(new BlockFormEvent(dest.getPieces(), dest));
        }
    }

    // route: the cells the piece just travelled through (null when it came from base)
    private void applyLanding(Piece piece, Cell dest, Player player, List<Cell> route) {
        if (dest == null) {
            return;
        }
        if (dest.isMysteryCell()) {
            Direction dirBefore = piece.getDirection();
            Optional<TeleportDestination> teleportOpt = mysteryManager.handleLanding(piece);
            Optional<Piece> captured = teleportOpt.flatMap(td -> ruleEngine.applyMysteryEffect(piece, td));
            Cell newPos = piece.getPosition();
            boolean refused = teleportOpt.isPresent() && teleportOpt.get() != TeleportDestination.BASE && newPos == dest;
            String destName = refused ? "BLOCKED" : teleportOpt.map(td -> actualDestination(td, dirBefore)).orElse("NONE");
            String effectDesc = refused ? piece.getName() + " cannot be teleported: an opposing blockade is in the way."
                    : teleportOpt.map(td -> OutputFormatter.buildEffectDescription(piece, td, dirBefore)).orElse("");
            eventBus.publish(new MysteryTeleportEvent(piece, player, dest, newPos, destName, effectDesc));
            captured.ifPresent(victim -> eventBus.publish(
                    new CaptureEvent(piece, victim, newPos, findPlayer(victim.getColour()).orElse(null))));
            checkBlockForm(piece, newPos);
        }
        announceRefusedHomeEntry(piece, route);
    }

    // T-7: the piece landed on or passed its approach cell but has not captured yet, so it stays on the path
    private void announceRefusedHomeEntry(Piece piece, List<Cell> route) {
        if (route == null || ruleEngine.getHomeEntryRule() == null || piece.getState() instanceof InHomeStraightState) {
            return;
        }
        boolean reachedApproach = route.contains(board.getApproachCell(piece.getColour()));
        int capturesNeeded = ruleEngine.getHomeEntryRule().capturesNeeded(piece);
        if (reachedApproach && capturesNeeded > 0) {
            eventBus.publish(new HomeEntryRejectedEvent(piece, capturesNeeded));
        }
    }

    private Optional<Player> findPlayer(Colour colour) {
        return players.stream().filter(p -> p.getColour() == colour).findFirst();
    }
}
