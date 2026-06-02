package com.ludot.engine;

import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.MoveResult;
import com.ludot.command.SkipMove;
import com.ludot.effects.TeleportDestination;
import com.ludot.events.BlockFormEvent;
import com.ludot.events.BlockMoveEvent;
import com.ludot.events.CaptureEvent;
import com.ludot.events.CoinTossEvent;
import com.ludot.events.EventBus;
import com.ludot.events.HomeEntryEvent;
import com.ludot.events.HomeReachEvent;
import com.ludot.events.MoveEvent;
import com.ludot.events.MysteryTeleportEvent;
import com.ludot.events.SkipTurnEvent;
import com.ludot.model.state.AtHomeState;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.Piece;
import com.ludot.model.state.PieceState;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.List;

// Executes validated Command objects and coordinates board state updates via the rule engine.
public class MoveExecutor {

    private final Board board;
    private final RuleEngine ruleEngine;
    private final MysteryManager mysteryManager;
    private final EventBus eventBus;

    public MoveExecutor(Board board, RuleEngine ruleEngine,
                        MysteryManager mysteryManager, EventBus eventBus) {
        this.board = board;
        this.ruleEngine = ruleEngine;
        this.mysteryManager = mysteryManager;
        this.eventBus = eventBus;
    }

    public MoveResult execute(Move move) {
        if (move instanceof MoveFromBase mfb)  return executeFromBase(mfb);
        if (move instanceof CaptureMove cm)    return executeCapture(cm);
        if (move instanceof BlockMove bm)      return executeBlock(bm);
        if (move instanceof MoveOnBoard mob)   return executeOnBoard(mob);
        if (move instanceof SkipMove)          return executeSkip(move);
        return move.execute();
    }

    private MoveResult executeFromBase(MoveFromBase mfb) {
        Piece piece = mfb.getPiece();
        eventBus.publish(new CoinTossEvent(piece, piece.getDirection()));
        MoveResult result = mfb.execute();
        Cell dest = piece.getPosition();
        eventBus.publish(new MoveEvent(piece, null, dest, 0));
        checkBlockForm(piece, dest);
        applyLanding(piece, dest);
        return result;
    }

    private MoveResult executeOnBoard(MoveOnBoard mob) {
        Piece piece = mob.getPiece();
        Cell from = piece.getPosition();
        PieceState stateBefore = piece.getState();
        MoveResult result = mob.execute();
        Cell dest = piece.getPosition();
        eventBus.publish(new MoveEvent(piece, from, dest, mob.getSteps()));
        if (piece.getState() instanceof AtHomeState) {
            eventBus.publish(new HomeReachEvent(piece));
            return result;
        }
        checkBlockForm(piece, dest);
        applyLanding(piece, dest);
        if (!(stateBefore instanceof InHomeStraightState)
                && piece.getState() instanceof InHomeStraightState) {
            eventBus.publish(new HomeEntryEvent(piece));
        }
        return result;
    }

    private MoveResult executeCapture(CaptureMove cm) {
        Piece attacker = cm.getAttacker();
        Cell from = attacker.getPosition();
        Cell targetCell = cm.getTargetCell();
        Piece victim = cm.execute().getCaptured(); // execute moves attacker and sends victim to base
        eventBus.publish(new CaptureEvent(attacker, victim, targetCell));
        eventBus.publish(new MoveEvent(attacker, from, attacker.getPosition(), cm.getSteps()));
        // re-construct result with grantsBonusRoll=true
        MoveResult result = new MoveResult(true, victim, false, true,
                attacker.getName() + " captures " + victim.getName());
        applyLanding(attacker, attacker.getPosition());
        return result;
    }

    private MoveResult executeBlock(BlockMove bm) {
        List<Piece> block = bm.getBlock();
        Cell from = block.get(0).getPosition();
        MoveResult result = bm.execute();
        Cell dest = block.get(0).getPosition();
        eventBus.publish(new BlockMoveEvent(block, from, dest, bm.getSteps()));
        return result;
    }

    private MoveResult executeSkip(Move skip) {
        MoveResult result = skip.execute();
        // SkipTurnEvent is published by TurnManager which has the player reference
        return result;
    }

    // if the piece's destination now holds a block (just formed), publish BlockFormEvent
    private void checkBlockForm(Piece piece, Cell dest) {
        if (dest != null && dest.hasBlock() && dest.getPieces().contains(piece)
                && dest.getBlockSize() >= 2) {
            eventBus.publish(new BlockFormEvent(dest.getPieces(), dest));
        }
    }

    // applies mystery effect if cell is/was mystery, then home-straight entry
    private void applyLanding(Piece piece, Cell dest) {
        if (dest == null) return;
        if (dest.isMysteryCell()) {
            Cell mysteryCell = dest;
            TeleportDestination teleportDest = mysteryManager.handleLanding(piece);
            ruleEngine.applyMysteryEffect(piece, teleportDest);
            Cell newPos = piece.getPosition();
            String effectName = teleportDest != null ? teleportDest.name() : "NONE";
            eventBus.publish(new MysteryTeleportEvent(piece, mysteryCell, newPos, effectName));
        }
        ruleEngine.applyLandingEffects(piece, piece.getPosition() != null ? piece.getPosition() : dest);
    }

    // legacy stubs retained for compatibility
    public void moveFromBase(Piece piece) {}
    public void moveOnBoard(Piece piece, int steps) {}
    public void handleCapture(Piece attacker, Piece victim) {}
    public void handleBlockMovement(List<Piece> block, int steps) {}
}
