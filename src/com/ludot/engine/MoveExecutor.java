package com.ludot.engine;

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
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.EventBus;
import com.ludot.events.home.HomeEntryEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.events.mystery.MysteryTeleportEvent;
import com.ludot.events.move.SkipTurnEvent;
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
import java.util.List;
import java.util.Optional;

/**
 * Executes validated Command objects and coordinates board-state updates via the rule engine.
 * Implements the Command pattern as the invoker that dispatches each Move subtype to its handler.
 */
public class MoveExecutor {

    private final Board board;
    private final RuleEngine ruleEngine;
    private final MysteryManager mysteryManager;
    private final EventBus eventBus;
    private final List<Player> players;

    /** Constructs a MoveExecutor without a player list, suitable for unit tests that do not need capture events. */
    public MoveExecutor(Board board, RuleEngine ruleEngine,
                        MysteryManager mysteryManager, EventBus eventBus) {
        this(board, ruleEngine, mysteryManager, eventBus, List.of());
    }

    /** Constructs a fully wired MoveExecutor with player-lookup support for capture event publishing. */
    public MoveExecutor(Board board, RuleEngine ruleEngine,
                        MysteryManager mysteryManager, EventBus eventBus, List<Player> players) {
        this.board = board;
        this.ruleEngine = ruleEngine;
        this.mysteryManager = mysteryManager;
        this.eventBus = eventBus;
        this.players = players;
    }

    /** Executes a Move without a player context; provided for backward-compatibility with tests. */
    public MoveResult execute(Move move) {
        return execute(move, null);
    }

    /** Dispatches the given Move to the appropriate handler and returns the resulting MoveResult. */
    public MoveResult execute(Move move, Player player) {
        if (move instanceof MoveFromBase mfb)     return executeFromBase(mfb, player);
        if (move instanceof CaptureMove cm)        return executeCapture(cm, player);
        if (move instanceof BlockCaptureMove bcm)  return executeBlockCapture(bcm, player);
        if (move instanceof BlockMove bm)          return executeBlock(bm);
        if (move instanceof MoveOnBoard mob)       return executeOnBoard(mob, player);
        if (move instanceof SkipMove)              return executeSkip(move);
        return move.execute();
    }

    private MoveResult executeFromBase(MoveFromBase mfb, Player player) {
        Piece piece = mfb.getPiece();
        eventBus.publish(new CoinTossEvent(piece, piece.getDirection()));
        MoveResult result = mfb.execute();
        Cell dest = piece.getPosition();
        eventBus.publish(new MoveEvent(player, piece, null, dest, 0));
        checkBlockForm(piece, dest);
        applyLanding(piece, dest, player);
        return result;
    }

    private MoveResult executeOnBoard(MoveOnBoard mob, Player player) {
        Piece piece = mob.getPiece();
        Cell from = piece.getPosition();
        PieceState stateBefore = piece.getState();
        MoveResult result = mob.execute();
        Cell dest = piece.getPosition();
        eventBus.publish(new MoveEvent(player, piece, from, dest, mob.getSteps()));
        if (piece.getState() instanceof AtHomeState) {
            eventBus.publish(new HomeReachEvent(piece));
            return result;
        }
        checkBlockForm(piece, dest);
        applyLanding(piece, dest, player);
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
        MoveResult cmdResult = cm.execute(); // moves attacker to target cell
        Piece victim = cmdResult.getCaptured();
        ruleEngine.getCaptureRule().capture(attacker, victim); // victim reset + T-9 + capture count
        Player victimPlayer = findPlayer(victim.getColour()).orElse(null);
        eventBus.publish(new CaptureEvent(attacker, victim, targetCell, victimPlayer));
        eventBus.publish(new MoveEvent(player, attacker, from, attacker.getPosition(), cm.getSteps()));
        MoveResult result = new MoveResult(true, victim, false, true,
                attacker.getName() + " captures " + victim.getName());
        applyLanding(attacker, attacker.getPosition(), player);
        return result;
    }

    private MoveResult executeBlock(BlockMove bm) {
        List<Piece> block = bm.getBlock();
        Cell from = block.get(0).getPosition();
        MoveResult result = bm.execute();
        Cell dest = block.get(0).getPosition();
        eventBus.publish(new BlockMoveEvent(block, from, dest, bm.getSteps(), bm.getOriginalRoll()));
        return result;
    }

    // T-8: block-vs-block capture — attacker movement done by the command; victim reset delegated
    // to CaptureRule so rule-variant logic (T-9 full reset vs classic send-to-base) is respected.
    private MoveResult executeBlockCapture(BlockCaptureMove bcm, Player player) {
        Cell from = bcm.getAttackers().get(0).getPosition();
        bcm.execute(); // moves all attackers, increments their captureCount
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

    private void checkBlockForm(Piece piece, Cell dest) {
        if (dest != null && dest.hasBlock() && dest.getPieces().contains(piece)
                && dest.getBlockSize() >= 2) {
            // T-5: save each piece's direction at the moment of block formation so it can be
            // restored when the block breaks. Only set if not already stored (handles 3rd piece joining).
            for (Piece p : dest.getPieces()) {
                if (p.getOriginalDirection() == null) {
                    p.setOriginalDirection(p.getDirection());
                }
            }
            eventBus.publish(new BlockFormEvent(dest.getPieces(), dest));
        }
    }

    private void applyLanding(Piece piece, Cell dest, Player player) {
        if (dest == null) return;
        if (dest.isMysteryCell()) {
            Direction dirBefore = piece.getDirection();
            Optional<TeleportDestination> teleportOpt = mysteryManager.handleLanding(piece);
            teleportOpt.ifPresent(td -> ruleEngine.applyMysteryEffect(piece, td));
            Cell newPos = piece.getPosition();
            String destName = teleportOpt.map(TeleportDestination::name).orElse("NONE");
            String effectDesc = teleportOpt
                    .map(td -> OutputFormatter.buildEffectDescription(piece, td, dirBefore))
                    .orElse("");
            eventBus.publish(new MysteryTeleportEvent(piece, player, dest, newPos, destName, effectDesc));
        }
        ruleEngine.applyLandingEffects(piece);
    }


    private Optional<Player> findPlayer(Colour colour) {
        return players.stream().filter(p -> p.getColour() == colour).findFirst();
    }
}
