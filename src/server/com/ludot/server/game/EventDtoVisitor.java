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
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.ArrayList;
import java.util.List;

/**
 * Observer + Visitor: listens to one game's EventBus and records every event as an {@link EventDto}
 * (type, who, where, and the Phase 1 narration). {@link #drain()} hands over what happened since the last call.
 */
final class EventDtoVisitor implements GameEventListener, EventVisitor {

    private final OutputFormatter formatter = new OutputFormatter();
    private final List<EventDto> collected = new ArrayList<>();

    @Override
    public void onEvent(GameEvent event) {
        event.accept(this);
    }

    /** Returns the events recorded since the previous call and clears the buffer. */
    List<EventDto> drain() {
        List<EventDto> drained = List.copyOf(collected);
        collected.clear();
        return drained;
    }

    private void add(EventType type, Colour colour, Piece piece, Cell from, Cell to, String text) {
        collected.add(new EventDto(type, colour, piece == null ? null : DtoMapper.pieceIndex(piece),
                DtoMapper.cellId(from), DtoMapper.cellId(to), text == null ? "" : text));
    }

    private static Colour of(Player player) {
        return DtoMapper.colour(player.getColour());
    }

    private static Colour of(Piece piece) {
        return DtoMapper.colour(piece.getColour());
    }

    @Override
    public void visit(DiceRollEvent e) {
        add(EventType.DICE_ROLL, of(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(CoinTossEvent e) {
        add(EventType.COIN_TOSS, of(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(TripleSixEvent e) {
        add(EventType.TRIPLE_SIX, of(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(MoveEvent e) {
        add(EventType.MOVE, of(e.player()), e.piece(), e.from(), e.to(), formatter.format(e));
    }

    @Override
    public void visit(CaptureEvent e) {
        add(EventType.CAPTURE, of(e.attacker()), e.attacker(), null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(SkipTurnEvent e) {
        add(EventType.SKIP_TURN, of(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(PieceBlockedEvent e) {
        add(EventType.PIECE_BLOCKED, of(e.blocked()), e.blocked(), e.from(), e.intendedDest(), formatter.format(e));
    }

    @Override
    public void visit(ApproachPassEvent e) {
        add(EventType.APPROACH_PASS, of(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(BlockFormEvent e) {
        add(EventType.BLOCK_FORM, of(e.block().get(0)), e.block().get(0), null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(BlockMoveEvent e) {
        add(EventType.BLOCK_MOVE, of(e.block().get(0)), e.block().get(0), e.from(), e.to(), formatter.format(e));
    }

    @Override
    public void visit(BlockBreakEvent e) {
        add(EventType.BLOCK_BREAK, of(e.block().get(0)), e.block().get(0), e.cell(), null, formatter.format(e));
    }

    @Override
    public void visit(HomeEntryEvent e) {
        add(EventType.HOME_ENTRY, of(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(HomeReachEvent e) {
        add(EventType.HOME_REACH, of(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(HomeEntryRejectedEvent e) {
        add(EventType.HOME_ENTRY_REJECTED, of(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(MysterySpawnEvent e) {
        add(EventType.MYSTERY_SPAWN, null, null, null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(MysteryTickEvent e) {
        add(EventType.MYSTERY_TICK, null, null, null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(MysteryExpireEvent e) {
        add(EventType.MYSTERY_EXPIRE, null, null, null, e.cell(), formatter.format(e));
    }

    @Override
    public void visit(MysteryTeleportEvent e) {
        add(EventType.MYSTERY_TELEPORT, of(e.player()), e.piece(), e.from(), e.to(), formatter.format(e));
    }

    @Override
    public void visit(EffectAppliedEvent e) {
        add(EventType.EFFECT_APPLIED, of(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(EffectExpiredEvent e) {
        add(EventType.EFFECT_EXPIRED, of(e.piece()), e.piece(), null, null, formatter.format(e));
    }

    @Override
    public void visit(GameStartEvent e) {
        add(EventType.GAME_START, null, null, null, null, formatter.format(e));
    }

    @Override
    public void visit(TurnStartEvent e) {
        add(EventType.TURN_START, of(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(RollOffEvent e) {
        add(EventType.ROLL_OFF, of(e.player()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(TurnOrderEvent e) {
        add(EventType.TURN_ORDER, of(e.first()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(RoundEndEvent e) {
        add(EventType.ROUND_END, null, null, null, null, formatter.format(e));
    }

    @Override
    public void visit(WinEvent e) {
        add(EventType.WIN, of(e.winner()), null, null, null, formatter.format(e));
    }

    @Override
    public void visit(BriefingEscapeEvent e) {
        add(EventType.BRIEFING_ESCAPE, of(e.player()), e.piece(), null, null, formatter.format(e));
    }
}
