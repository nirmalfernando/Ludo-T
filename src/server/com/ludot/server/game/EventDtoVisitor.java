package com.ludot.server.game;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.events.GameEventListener;
import com.ludot.events.OutputFormatter;
import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.block.BlockMoveEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.events.effect.EffectAppliedEvent;
import com.ludot.events.effect.EffectExpiredEvent;
import com.ludot.events.flow.BriefingEscapeEvent;
import com.ludot.events.flow.GameStartEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.events.flow.TurnStartEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.events.home.HomeEntryEvent;
import com.ludot.events.home.HomeEntryRejectedEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.ApproachPassEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.events.move.PieceBlockedEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.events.mystery.MysteryExpireEvent;
import com.ludot.events.mystery.MysterySpawnEvent;
import com.ludot.events.mystery.MysteryTeleportEvent;
import com.ludot.events.mystery.MysteryTickEvent;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.List;

/**
 * Observer + Visitor: listens to one game's EventBus and records every event as an {@link EventDto}
 * (type, who, where, and the Phase 1 narration). {@link #drain()} hands over what happened since the last call.
 * Dispatch to the right kind of event is the visitor's job; building and keeping the DTOs is the
 * {@link EventRecorder}'s.
 */
final class EventDtoVisitor implements GameEventListener, EventVisitor {

    private final OutputFormatter formatter = new OutputFormatter();
    private final EventRecorder recorder = new EventRecorder();

    @Override
    public void onEvent(GameEvent event) {
        event.accept(this);
    }

    /** Returns the events recorded since the previous call and clears the buffer. */
    List<EventDto> drain() {
        return recorder.drain();
    }

    @Override
    public void visit(DiceRollEvent e) {
        recorder.add(EventType.DICE_ROLL, EventRecorder.colourOf(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(CoinTossEvent e) {
        recorder.add(EventType.COIN_TOSS, EventRecorder.colourOf(e.piece()), e.piece(), null, null, formatter.format(e),
                e.direction() == com.ludot.model.Direction.CLOCKWISE ? 1 : 0);
    }

    @Override
    public void visit(TripleSixEvent e) {
        recorder.add(EventType.TRIPLE_SIX, EventRecorder.colourOf(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(MoveEvent e) {
        recorder.add(EventType.MOVE, EventRecorder.colourOf(e.player()), e.piece(), e.from(), e.to(), formatter.format(e));
    }

    @Override
    public void visit(CaptureEvent e) {
        recorder.add(EventType.CAPTURE, EventRecorder.colourOf(e.attacker()), e.attacker(), null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(SkipTurnEvent e) {
        recorder.add(EventType.SKIP_TURN, EventRecorder.colourOf(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(PieceBlockedEvent e) {
        recorder.add(EventType.PIECE_BLOCKED, EventRecorder.colourOf(e.blocked()), e.blocked(), e.from(), e.intendedDest(), formatter.format(e));
    }

    @Override
    public void visit(ApproachPassEvent e) {
        recorder.add(EventType.APPROACH_PASS, EventRecorder.colourOf(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(BlockFormEvent e) {
        recorder.add(EventType.BLOCK_FORM, EventRecorder.colourOf(e.block().get(0)), e.block().get(0), null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(BlockMoveEvent e) {
        recorder.add(EventType.BLOCK_MOVE, EventRecorder.colourOf(e.block().get(0)), e.block().get(0), e.from(), e.to(), formatter.format(e));
    }

    @Override
    public void visit(BlockBreakEvent e) {
        recorder.add(EventType.BLOCK_BREAK, EventRecorder.colourOf(e.block().get(0)), e.block().get(0), e.cell(), null, formatter.format(e));
    }

    @Override
    public void visit(HomeEntryEvent e) {
        recorder.add(EventType.HOME_ENTRY, EventRecorder.colourOf(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(HomeReachEvent e) {
        recorder.add(EventType.HOME_REACH, EventRecorder.colourOf(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(HomeEntryRejectedEvent e) {
        recorder.add(EventType.HOME_ENTRY_REJECTED, EventRecorder.colourOf(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(MysterySpawnEvent e) {
        recorder.add(EventType.MYSTERY_SPAWN, null, null, null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(MysteryTickEvent e) {
        recorder.add(EventType.MYSTERY_TICK, null, null, null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(MysteryExpireEvent e) {
        recorder.add(EventType.MYSTERY_EXPIRE, null, null, null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(MysteryTeleportEvent e) {
        recorder.add(EventType.MYSTERY_TELEPORT, EventRecorder.colourOf(e.player()), e.piece(), e.from(), e.to(), formatter.format(e));
    }

    @Override
    public void visit(EffectAppliedEvent e) {
        recorder.add(EventType.EFFECT_APPLIED, EventRecorder.colourOf(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(EffectExpiredEvent e) {
        recorder.add(EventType.EFFECT_EXPIRED, EventRecorder.colourOf(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(GameStartEvent e) {
        recorder.add(EventType.GAME_START, null, null, null, null, formatter.format(e));
    }

    @Override
    public void visit(TurnStartEvent e) {
        recorder.add(EventType.TURN_START, EventRecorder.colourOf(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(RollOffEvent e) {
        recorder.add(EventType.ROLL_OFF, EventRecorder.colourOf(e.player()), null, null, null, formatter.format(e), e.roll());
    }

    @Override
    public void visit(TurnOrderEvent e) {
        recorder.add(EventType.TURN_ORDER, EventRecorder.colourOf(e.first()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(RoundEndEvent e) {
        recorder.add(EventType.ROUND_END, null, null, null, null, formatter.format(e));
    }

    @Override
    public void visit(WinEvent e) {
        recorder.add(EventType.WIN, EventRecorder.colourOf(e.winner()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(BriefingEscapeEvent e) {
        recorder.add(EventType.BRIEFING_ESCAPE, EventRecorder.colourOf(e.player()), e.piece(), null, null, formatter.format(e));
    }
}
