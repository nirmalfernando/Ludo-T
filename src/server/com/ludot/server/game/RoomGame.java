package com.ludot.server.game;

import com.ludot.command.Move;
import com.ludot.engine.GameEngine;
import com.ludot.engine.GamePhase;
import com.ludot.engine.RollResult;
import com.ludot.events.EventBus;
import com.ludot.factory.ClassicRuleEngineFactory;
import com.ludot.factory.GameBuilder;
import com.ludot.factory.LudoTRuleEngineFactory;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Player;
import com.ludot.protocol.BoardDto;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.MoveKind;
import com.ludot.protocol.MoveOption;
import com.ludot.protocol.Phase;
import com.ludot.protocol.StateHash;
import com.ludot.protocol.TurnInfo;
import com.ludot.protocol.Variant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RoomGame {

    public record Step(List<EventDto> events, Integer rolled) {
    }

    private final GameEngine engine;
    private final EventDtoVisitor collector = new EventDtoVisitor();
    private Integer lastRoll;
    private List<MoveOption> options = List.of();

    public RoomGame(Variant variant, long seed) {
        EventBus bus = new EventBus();
        bus.subscribe(collector);
        this.engine = new GameBuilder()
                .withSeed(seed)
                .withPlayers(4)
                .withEventBus(bus)
                .withRuleEngineFactory(ruleFactory(variant))
                .build();
    }

    private static RuleEngineFactory ruleFactory(Variant variant) {
        return variant == Variant.CLASSIC ? new ClassicRuleEngineFactory() : new LudoTRuleEngineFactory();
    }

    public Step start() {
        engine.start();
        return step(null);
    }

    public Step open() {
        engine.beginRollOff();
        return step(null);
    }

    public Step rollOff(Colour colour) {
        int roll = engine.rollOff(DtoMapper.coreColour(colour));
        return step(roll);
    }

    public List<Colour> playOrder() {
        GamePhase now = engine.getPhase();
        if (now == GamePhase.NOT_STARTED || now == GamePhase.ROLL_OFF) {
            return List.of();
        }
        return engine.getPlayers().stream().map(p -> DtoMapper.colour(p.getColour())).toList();
    }

    public Step roll(Colour colour) {
        RollResult result = engine.roll(DtoMapper.coreColour(colour));
        lastRoll = result.roll();
        options = result.turnOver() ? List.of() : MoveOptionMapper.options(result.validMoves());
        Step step = step(result.roll());
        if (result.turnOver()) {
            lastRoll = null;
        }
        return step;
    }

    public Step applyMove(Colour colour, int optionIndex) {
        engine.applyMove(DtoMapper.coreColour(colour), optionIndex);
        options = List.of();
        if (engine.getPhase() != GamePhase.AWAITING_MOVE) {
            lastRoll = null;
        }
        return step(null);
    }

    private Step step(Integer rolled) {
        return new Step(collector.drain(), rolled);
    }

    public boolean isStarted() {
        return engine.getPhase() != GamePhase.NOT_STARTED;
    }

    public boolean isFinished() {
        return engine.getPhase() == GamePhase.FINISHED;
    }

    public Colour currentColour() {
        return DtoMapper.colour(engine.getCurrentPlayer().getColour());
    }

    public Phase phase() {
        return switch (engine.getPhase()) {
            case ROLL_OFF -> Phase.ROLL_OFF;
            case AWAITING_MOVE -> Phase.AWAITING_MOVE;
            case FINISHED -> Phase.FINISHED;
            case NOT_STARTED, AWAITING_ROLL -> Phase.AWAITING_ROLL;
        };
    }

    // requests quote it so stale ones can be rejected
    public int turnNumber() {
        return engine.getTurnNumber();
    }

    public Integer lastRoll() {
        return lastRoll;
    }

    // the legal moves for the current roll; empty unless a move is awaited
    public List<MoveOption> options() {
        return options;
    }

    public boolean onlySkipAvailable() {
        return options.size() == 1 && options.get(0).kind() == MoveKind.SKIP;
    }

    public TurnInfo turnInfo(Long deadlineEpochMs) {
        if (!isStarted()) {
            return null;
        }
        return new TurnInfo(turnNumber(), currentColour(), phase(), lastRoll, deadlineEpochMs);
    }

    public int strategyChoice() {
        Player player = engine.getCurrentPlayer();
        List<Move> offered = engine.getPendingMoves();
        Move chosen = player.getStrategy().decideMoves(
                engine.currentGameState(), lastRoll, player.getPieces(), offered);
        return offered.indexOf(chosen);
    }

    public BoardDto board() {
        return DtoMapper.board(engine);
    }

    public long stateHash() {
        return StateHash.of(board(), turnNumber());
    }

    public List<Colour> standings() {
        List<Player> ranked = new ArrayList<>(engine.getPlayers());
        ranked.sort(Comparator.comparing((Player p) -> !p.hasWon())
                .thenComparing(p -> -p.getPiecesAtHome().size()));
        List<Colour> standings = new ArrayList<>();
        ranked.forEach(p -> standings.add(DtoMapper.colour(p.getColour())));
        return List.copyOf(standings);
    }
}
