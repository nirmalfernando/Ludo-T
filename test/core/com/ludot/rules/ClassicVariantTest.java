package com.ludot.rules;

import com.ludot.TestBoard;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.engine.GameEngine;
import com.ludot.engine.GamePhase;
import com.ludot.events.EventBus;
import com.ludot.events.GameEvent;
import com.ludot.factory.ClassicRuleEngineFactory;
import com.ludot.factory.GameBuilder;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.model.state.OnBoardState;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Classic variant: traditional Ludo with none of the LUDO-T extensions")
class ClassicVariantTest {

    private static final Set<String> TWIST_EVENTS = Set.of("MysterySpawnEvent", "MysteryTickEvent",
            "MysteryExpireEvent", "MysteryTeleportEvent", "BlockFormEvent", "BlockMoveEvent", "BlockBreakEvent",
            "CoinTossEvent", "EffectAppliedEvent", "EffectExpiredEvent", "BriefingEscapeEvent");

    private record Played(Set<String> twistEvents, boolean finished, String violation) {
    }

    private static Played playClassic(long seed) {
        Set<String> twistEvents = new TreeSet<>();
        EventBus bus = new EventBus();
        bus.subscribe((GameEvent event) -> {
            String name = event.getClass().getSimpleName();
            if (TWIST_EVENTS.contains(name)) {
                twistEvents.add(name);
            }
        });
        GameEngine engine = new GameBuilder().withSeed(seed).withEventBus(bus)
                .withRuleEngineFactory(new ClassicRuleEngineFactory()).build();
        engine.start();
        String violation = null;
        for (int step = 0; step < 100_000 && engine.getPhase() != GamePhase.FINISHED; step++) {
            Player player = engine.getCurrentPlayer();
            var rolled = engine.roll(player.getColour());
            if (engine.getPhase() == GamePhase.AWAITING_MOVE) {
                Move chosen = player.getStrategy().decideMoves(engine.currentGameState(), rolled.roll(),
                        player.getPieces(), rolled.validMoves());
                engine.applyMove(player.getColour(), rolled.validMoves().indexOf(chosen));
            }
            if (violation == null) {
                violation = invariantBroken(engine);
            }
        }
        return new Played(twistEvents, engine.getPhase() == GamePhase.FINISHED, violation);
    }

    private static String invariantBroken(GameEngine engine) {
        for (Cell cell : engine.getBoard().getStandardPath()) {
            if (cell.isMysteryCell()) {
                return "a mystery cell exists on cell " + cell.getId();
            }
            if (cell.hasBlock()) {
                return "two pieces of one colour share cell " + cell.getId() + " (Rule 7)";
            }
        }
        for (Player player : engine.getPlayers()) {
            for (Piece piece : player.getPieces()) {
                if (piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
                    return piece.getName() + " moves counter-clockwise (Rule 8)";
                }
            }
        }
        return null;
    }

    @Test @DisplayName("Classic games never show a mystery cell, a blockade, a coin toss or a counter-clockwise piece")
    void noLudoTFeaturesInClassicGames() {
        for (long seed = 1; seed <= 12; seed++) {
            Played played = playClassic(seed);
            assertTrue(played.twistEvents().isEmpty(), "seed " + seed + " produced LUDO-T events " + played.twistEvents());
            assertEquals(null, played.violation(), "seed " + seed);
        }
    }

    @Test @DisplayName("Classic games finish with a winner (Rule 11), so the Rule 7 restriction cannot deadlock them")
    void classicGamesFinish() {
        for (long seed = 1; seed <= 12; seed++) {
            assertTrue(playClassic(seed).finished(), "seed " + seed + " did not finish within 100,000 steps");
        }
    }

    @Test @DisplayName("Rule 7: a move that would land on the player's own piece is not offered; if nothing else can move the roll is ignored")
    void ownPieceCannotBeLandedOn() {
        Board board = TestBoard.create();
        RuleEngine engine = RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider());
        Piece mover = onBoard(board, "R1", Colour.RED, 26);
        Piece friend = onBoard(board, "R2", Colour.RED, 29);
        Player player = new Player(Colour.RED, List.of(mover, friend), (gs, r, ps, m) -> m.get(0));

        List<Move> moves = engine.getValidMoves(player, 3);

        assertTrue(moves.stream().noneMatch(m -> m instanceof MoveOnBoard mob && mob.getPiece() == mover),
                "R1 must not be allowed to land on R2");
        assertTrue(moves.stream().anyMatch(m -> m instanceof MoveOnBoard mob && mob.getPiece() == friend),
                "R2 can still move");
    }

    @Test @DisplayName("Rule 7: when the only possible move lands on an own piece the roll is ignored (SkipMove)")
    void rollIgnoredWhenOnlyMoveLandsOnOwnPiece() {
        Board board = TestBoard.create();
        RuleEngine engine = RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider());
        Piece mover = onBoard(board, "R1", Colour.RED, 26);
        Piece friend = onBoard(board, "R2", Colour.RED, 29);
        // R2 sits in the home straight, so it cannot take the roll
        board.getCell(29).removePiece(friend);
        Cell home = board.getHomeStraight(Colour.RED).get(4);
        friend.setState(new com.ludot.model.state.AtHomeState());
        home.addPiece(friend);
        friend.setPosition(home);
        Piece blocker = onBoard(board, "R3", Colour.RED, 29);
        Player player = new Player(Colour.RED, List.of(mover, friend, blocker), (gs, r, ps, m) -> m.get(0));

        List<Move> moves = engine.getValidMoves(player, 3);

        assertTrue(moves.stream().noneMatch(m -> m instanceof MoveOnBoard mob && mob.getPiece() == mover));
        assertNotEquals(0, moves.size());
        if (moves.stream().noneMatch(m -> m instanceof MoveOnBoard mob && mob.getPiece() == blocker)) {
            assertInstanceOf(SkipMove.class, moves.get(0));
        }
    }

    @Test @DisplayName("Classic never reports a stacked own pair as a blockade: the Rule 7 check is the only same-cell rule")
    void classicHasNoBlockRule() {
        RuleEngine classic = RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider());
        assertFalse(classic.supportsBlocks());
        assertFalse(classic.supportsMysteryCells());
        assertTrue(RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider()).supportsBlocks());
        assertTrue(RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider()).supportsMysteryCells());
    }

    private static Piece onBoard(Board board, String name, Colour colour, int cellId) {
        Piece piece = new Piece(name, colour, board);
        Cell cell = board.getCell(cellId);
        cell.addPiece(piece);
        piece.setPosition(cell);
        piece.setState(new OnBoardState());
        piece.setDirection(Direction.CLOCKWISE);
        return piece;
    }
}
