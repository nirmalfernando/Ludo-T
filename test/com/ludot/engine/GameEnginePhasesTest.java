package com.ludot.engine;

import com.ludot.command.Move;
import com.ludot.events.EventBus;
import com.ludot.events.GameEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.factory.GameBuilder;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameEngine phased turns")
class GameEnginePhasesTest {

    private List<GameEvent> events;

    @BeforeEach void setUp() {
        events = new ArrayList<>();
    }

    private GameEngine build(long seed) {
        events = new ArrayList<>();
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        return new GameBuilder().withSeed(seed).withEventBus(bus).withPlayers(4).build();
    }

    // rolls for the current player until a move choice is offered (a triple six can forfeit a turn)
    private RollResult rollUntilMoveOffered(GameEngine engine) {
        for (int i = 0; i < 50; i++) {
            RollResult rolled = engine.roll(engine.getCurrentPlayer().getColour());
            if (engine.getPhase() == GamePhase.AWAITING_MOVE) {
                return rolled;
            }
        }
        throw new AssertionError("no move was ever offered");
    }

    @Test @DisplayName("a new engine has not started and rejects a roll")
    void notStartedRejectsRoll() {
        GameEngine engine = build(1);
        assertEquals(GamePhase.NOT_STARTED, engine.getPhase());
        assertThrows(IllegalStateException.class, () -> engine.roll(Colour.RED));
    }

    @Test @DisplayName("start opens the first player's turn awaiting a roll")
    void startAwaitsRoll() {
        GameEngine engine = build(1);
        engine.start();
        assertEquals(GamePhase.AWAITING_ROLL, engine.getPhase());
        assertEquals(0, engine.getTurnNumber());
        assertThrows(IllegalStateException.class, engine::start);
    }

    @Test @DisplayName("only the current player may roll, and a rejected roll changes nothing")
    void wrongPlayerCannotRoll() {
        GameEngine engine = build(1);
        engine.start();
        Colour current = engine.getCurrentPlayer().getColour();
        Colour other = current == Colour.RED ? Colour.BLUE : Colour.RED;
        int before = events.size();
        assertThrows(IllegalStateException.class, () -> engine.roll(other));
        assertEquals(before, events.size());
        assertEquals(GamePhase.AWAITING_ROLL, engine.getPhase());
        assertEquals(0, engine.getTurnNumber());
    }

    @Test @DisplayName("a move cannot be applied before rolling")
    void moveBeforeRollRejected() {
        GameEngine engine = build(1);
        engine.start();
        assertThrows(IllegalStateException.class,
                () -> engine.applyMove(engine.getCurrentPlayer().getColour(), 0));
    }

    @Test @DisplayName("a roll offers moves and a second roll is rejected until the move is applied")
    void rollOffersMovesThenBlocksSecondRoll() {
        GameEngine engine = build(1);
        engine.start();
        RollResult rolled = rollUntilMoveOffered(engine);
        assertFalse(rolled.validMoves().isEmpty());
        assertEquals(rolled.validMoves(), engine.getPendingMoves());
        assertThrows(IllegalStateException.class,
                () -> engine.roll(engine.getCurrentPlayer().getColour()));
    }

    @Test @DisplayName("an out-of-range move index is rejected and the phase is kept")
    void badMoveIndexRejected() {
        GameEngine engine = build(2);
        engine.start();
        RollResult rolled = rollUntilMoveOffered(engine);
        Colour colour = engine.getCurrentPlayer().getColour();
        assertThrows(IllegalArgumentException.class, () -> engine.applyMove(colour, -1));
        assertThrows(IllegalArgumentException.class,
                () -> engine.applyMove(colour, rolled.validMoves().size()));
        assertEquals(GamePhase.AWAITING_MOVE, engine.getPhase());
    }

    @Test @DisplayName("every applied roll and move advances the turn number by one")
    void turnNumberCountsSteps() {
        GameEngine engine = build(3);
        engine.start();
        int expected = 0;
        for (int i = 0; i < 20 && engine.getPhase() != GamePhase.FINISHED; i++) {
            Colour colour = engine.getCurrentPlayer().getColour();
            RollResult rolled = engine.roll(colour);
            assertEquals(++expected, engine.getTurnNumber());
            if (engine.getPhase() == GamePhase.AWAITING_MOVE) {
                engine.applyMove(colour, 0);
                assertEquals(++expected, engine.getTurnNumber());
            }
        }
    }

    @Test @DisplayName("play passes to the next seat once a turn is over")
    void turnPassesToNextSeat() {
        GameEngine engine = build(4);
        engine.start();
        Player first = engine.getCurrentPlayer();
        for (int i = 0; i < 50 && engine.getCurrentPlayer() == first; i++) {
            Colour colour = engine.getCurrentPlayer().getColour();
            engine.roll(colour);
            if (engine.getPhase() == GamePhase.AWAITING_MOVE) {
                engine.applyMove(colour, 0);
            }
        }
        assertNotSame(first, engine.getCurrentPlayer());
    }

    @Test @DisplayName("driving the phases by hand with each strategy reproduces run() exactly")
    void manualDriveMatchesRun() {
        for (long seed : new long[]{1, 2, 2069}) {
            GameEngine auto = build(seed);
            auto.run();
            List<String> autoEvents = events.stream().map(e -> e.getClass().getSimpleName()).toList();

            GameEngine manual = build(seed);
            manual.start();
            while (manual.getPhase() != GamePhase.FINISHED) {
                Player p = manual.getCurrentPlayer();
                RollResult rolled = manual.roll(p.getColour());
                if (manual.getPhase() == GamePhase.AWAITING_MOVE) {
                    Move chosen = p.getStrategy().decideMoves(
                            manual.currentGameState(), rolled.roll(), p.getPieces(), rolled.validMoves());
                    manual.applyMove(p.getColour(), rolled.validMoves().indexOf(chosen));
                }
            }
            List<String> manualEvents = events.stream().map(e -> e.getClass().getSimpleName()).toList();

            assertTrue(autoEvents.size() > 100, "game should be non-trivial");
            assertEquals(autoEvents, manualEvents, "seed " + seed);
            assertTrue(manual.checkWinCondition());
        }
    }

    @Test @DisplayName("a finished game rejects further rolls and publishes exactly one WinEvent")
    void finishedGameRejectsRolls() {
        GameEngine engine = build(1);
        engine.run();
        assertEquals(GamePhase.FINISHED, engine.getPhase());
        assertEquals(1, events.stream().filter(e -> e instanceof WinEvent).count());
        assertThrows(IllegalStateException.class,
                () -> engine.roll(engine.getCurrentPlayer().getColour()));
    }
}
