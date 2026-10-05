package com.ludot.engine;

import com.ludot.TestBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.dice.SeededRandomProvider;
import com.ludot.events.EventBus;
import com.ludot.events.GameEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.factory.GameBuilder;
import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameEngine")
class GameEngineTest {

    private Board board;

    private List<GameEvent> events;
    private EventBus bus;

    @BeforeEach void setUp() {
        board = TestBoard.create();
        events = new ArrayList<>();
        bus = new EventBus();
        bus.subscribe(events::add);
    }

    
    // ── helpers ──────────────────────────────────────────────────────────────

    private GameEngine buildWith(int... diceRolls) {
        FixedRandomProvider random = new FixedRandomProvider().addDice(diceRolls);
        return new GameBuilder().withRandom(random).withEventBus(bus).withPlayers(4).build();
    }

    private List<RollOffEvent> rollOffEvents() {
        return events.stream()
                .filter(e -> e instanceof RollOffEvent)
                .map(e -> (RollOffEvent) e)
                .toList();
    }

    private TurnOrderEvent turnOrderEvent() {
        return events.stream()
                .filter(e -> e instanceof TurnOrderEvent)
                .map(e -> (TurnOrderEvent) e)
                .findFirst()
                .orElse(null);
    }

    // ── checkWinCondition ────────────────────────────────────────────────────

    @Test @DisplayName("checkWinCondition false when no player has won")
    void noWinYet() {
        GameEngine engine = new GameBuilder().withSeed(1).withPlayers(4).build();
        assertFalse(engine.checkWinCondition());
    }

    // ── roll-off: event publishing ───────────────────────────────────────────

    @Test @DisplayName("determineFirstPlayer publishes one RollOffEvent per player")
    void rollOffEventCountMatchesPlayerCount() {
        // PlayerFactory creates [RED, GREEN, YELLOW, BLUE] — four rolls consumed
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        assertEquals(4, rollOffEvents().size());
    }

    @Test @DisplayName("RollOffEvents carry the correct roll values")
    void rollOffEventsHaveCorrectValues() {
        // RED=3, GREEN=5, YELLOW=1, BLUE=4
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        List<RollOffEvent> ro = rollOffEvents();
        assertEquals(3, ro.get(0).roll()); // RED rolled 3
        assertEquals(5, ro.get(1).roll()); // GREEN rolled 5
        assertEquals(1, ro.get(2).roll()); // YELLOW rolled 1
        assertEquals(4, ro.get(3).roll()); // BLUE rolled 4
    }

    @Test @DisplayName("RollOffEvents carry the correct player colours")
    void rollOffEventsHaveCorrectPlayers() {
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        List<RollOffEvent> ro = rollOffEvents();
        assertEquals(Colour.RED,    ro.get(0).player().getColour());
        assertEquals(Colour.GREEN,  ro.get(1).player().getColour());
        assertEquals(Colour.YELLOW, ro.get(2).player().getColour());
        assertEquals(Colour.BLUE,   ro.get(3).player().getColour());
    }

    @Test @DisplayName("determineFirstPlayer publishes exactly one TurnOrderEvent")
    void turnOrderEventPublishedOnce() {
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        long count = events.stream().filter(e -> e instanceof TurnOrderEvent).count();
        assertEquals(1, count);
    }

    // ── roll-off: ordering ───────────────────────────────────────────────────

    @Test @DisplayName("highest roller becomes the first player")
    void highestRollerGoesFirst() {
        // RED=3, GREEN=5, YELLOW=1, BLUE=4 → GREEN wins with 5
        GameEngine engine = buildWith(3, 5, 1, 4);
        Player first = engine.determineFirstPlayer();
        assertEquals(Colour.GREEN, first.getColour());
    }

    @Test @DisplayName("lowest roller becomes the last player")
    void lowestRollerGoesLast() {
        // RED=3, GREEN=5, YELLOW=1, BLUE=4 → YELLOW last with 1
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        List<Player> order = engine.getPlayers();
        assertEquals(Colour.YELLOW, order.get(3).getColour());
    }

    @Test @DisplayName("full play order is sorted highest roll to lowest")
    void fullOrderDescendingByRoll() {
        // RED=3, GREEN=5, YELLOW=1, BLUE=4 → GREEN(5), BLUE(4), RED(3), YELLOW(1)
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        List<Player> order = engine.getPlayers();
        assertEquals(Colour.GREEN,  order.get(0).getColour());
        assertEquals(Colour.BLUE,   order.get(1).getColour());
        assertEquals(Colour.RED,    order.get(2).getColour());
        assertEquals(Colour.YELLOW, order.get(3).getColour());
    }

    @Test @DisplayName("TurnOrderEvent first field matches the returned first player")
    void turnOrderEventFirstMatchesDetermineResult() {
        GameEngine engine = buildWith(3, 5, 1, 4);
        Player first = engine.determineFirstPlayer();
        TurnOrderEvent toe = turnOrderEvent();
        assertNotNull(toe);
        assertEquals(first.getColour(), toe.first().getColour());
    }

    @Test @DisplayName("TurnOrderEvent order list matches getPlayers() order")
    void turnOrderEventListMatchesPlayerOrder() {
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        TurnOrderEvent toe = turnOrderEvent();
        List<Player> engineOrder = engine.getPlayers();
        assertNotNull(toe);
        assertEquals(engineOrder.size(), toe.order().size());
        for (int i = 0; i < engineOrder.size(); i++) {
            assertEquals(engineOrder.get(i).getColour(), toe.order().get(i).getColour());
        }
    }

    // ── roll-off: tie-breaking ───────────────────────────────────────────────

    @Test @DisplayName("tied rolls within a group are sorted alphabetically")
    void tiedRollsSortedAlphabetically() {
        // RED=4, GREEN=4, YELLOW=2, BLUE=2
        // Tied at 4: BLUE not in this group; GREEN before RED alphabetically
        // Wait — BLUE=2 here so tied at 4 group is RED+GREEN → GREEN < RED alphabetically
        // Tied at 2: BLUE+YELLOW → BLUE < YELLOW alphabetically
        // Expected: GREEN, RED, BLUE, YELLOW
        GameEngine engine = buildWith(4, 4, 2, 2);
        engine.determineFirstPlayer();
        List<Player> order = engine.getPlayers();
        assertEquals(Colour.GREEN,  order.get(0).getColour());
        assertEquals(Colour.RED,    order.get(1).getColour());
        assertEquals(Colour.BLUE,   order.get(2).getColour());
        assertEquals(Colour.YELLOW, order.get(3).getColour());
    }

    @Test @DisplayName("all equal rolls orders players alphabetically by colour name")
    void allEqualRollsOrderedAlphabetically() {
        // All roll 3 → alphabetical: BLUE, GREEN, RED, YELLOW
        GameEngine engine = buildWith(3, 3, 3, 3);
        engine.determineFirstPlayer();
        List<Player> order = engine.getPlayers();
        assertEquals(Colour.BLUE,   order.get(0).getColour());
        assertEquals(Colour.GREEN,  order.get(1).getColour());
        assertEquals(Colour.RED,    order.get(2).getColour());
        assertEquals(Colour.YELLOW, order.get(3).getColour());
    }

    @Test @DisplayName("seed 55 produces all-equal rolls and alphabetical order")
    void seed55AllEqualAlphabetical() {
        // Seed 55 → all four players roll 2; expected order: BLUE, GREEN, RED, YELLOW
        GameEngine engine = new GameBuilder()
                .withRandom(new SeededRandomProvider(55))
                .withEventBus(bus)
                .withPlayers(4)
                .build();
        engine.determineFirstPlayer();

        // verify all four RollOff values are identical
        List<RollOffEvent> ro = rollOffEvents();
        assertEquals(4, ro.size());
        int firstRoll = ro.get(0).roll();
        assertTrue(ro.stream().allMatch(e -> e.roll() == firstRoll),
                "All roll-off rolls should be equal for seed 55");

        // verify alphabetical order
        List<Player> order = engine.getPlayers();
        assertEquals(Colour.BLUE,   order.get(0).getColour());
        assertEquals(Colour.GREEN,  order.get(1).getColour());
        assertEquals(Colour.RED,    order.get(2).getColour());
        assertEquals(Colour.YELLOW, order.get(3).getColour());
    }

    @Test @DisplayName("partial tie: tied players sorted alphabetically, non-tied by roll")
    void partialTieAlphabeticalWithinGroup() {
        // RED=4, GREEN=4, YELLOW=2, BLUE=4 → tied at 4: BLUE, GREEN, RED alphabetically; YELLOW last
        GameEngine engine = buildWith(4, 4, 2, 4);
        engine.determineFirstPlayer();
        List<Player> order = engine.getPlayers();
        assertEquals(Colour.BLUE,   order.get(0).getColour());
        assertEquals(Colour.GREEN,  order.get(1).getColour());
        assertEquals(Colour.RED,    order.get(2).getColour());
        assertEquals(Colour.YELLOW, order.get(3).getColour());
    }

    // ── roll-off: TurnOrderEvent roll results map ────────────────────────────

    @Test @DisplayName("TurnOrderEvent rollResults contains an entry for every player")
    void rollResultsMapContainsAllPlayers() {
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        TurnOrderEvent toe = turnOrderEvent();
        assertNotNull(toe);
        assertEquals(4, toe.rollResults().size());
    }

    @Test @DisplayName("TurnOrderEvent rollResults values match the published RollOffEvents")
    void rollResultsValuesMatchRollOffEvents() {
        // RED=3, GREEN=5, YELLOW=1, BLUE=4
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        TurnOrderEvent toe = turnOrderEvent();
        assertNotNull(toe);
        assertEquals(3, toe.rollResults().get(Colour.RED));
        assertEquals(5, toe.rollResults().get(Colour.GREEN));
        assertEquals(1, toe.rollResults().get(Colour.YELLOW));
        assertEquals(4, toe.rollResults().get(Colour.BLUE));
    }

    @Test @DisplayName("seed 55: TurnOrderEvent rollResults shows all equal values")
    void seed55RollResultsAllEqual() {
        GameEngine engine = new GameBuilder()
                .withRandom(new SeededRandomProvider(55))
                .withEventBus(bus)
                .withPlayers(4)
                .build();
        engine.determineFirstPlayer();
        TurnOrderEvent toe = turnOrderEvent();
        assertNotNull(toe);
        long distinct = toe.rollResults().values().stream().distinct().count();
        assertEquals(1, distinct, "All four rolls should be the same for seed 55");
    }

    // ── game completion ──────────────────────────────────────────────────────

    @Test @DisplayName("run completes, publishes WinEvent with a valid winner and positive round count")
    void runPublishesWinEvent() {
        board = TestBoard.create();
        GameEngine engine = new GameBuilder().withSeed(42).withEventBus(bus).withPlayers(4).build();
        assertDoesNotThrow(engine::run);

        WinEvent win = events.stream()
                .filter(e -> e instanceof WinEvent)
                .map(e -> (WinEvent) e)
                .findFirst()
                .orElse(null);

        assertNotNull(win, "A WinEvent must be published when the game ends");
        assertNotNull(win.winner(), "WinEvent must carry a non-null winner");
        assertTrue(win.totalRounds() > 0, "Game must last at least one round");
        assertTrue(engine.checkWinCondition(), "Win condition must be true after run() completes");
    }
}
