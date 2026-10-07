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

    @Test @DisplayName("the order of a round is clockwise (R, G, Y, B) starting from the highest roller")
    void orderIsClockwiseFromTheHighestRoller() {
        // RED=3, GREEN=5, YELLOW=1, BLUE=4 -> GREEN rolled highest, so GREEN, YELLOW, BLUE, RED
        GameEngine engine = buildWith(3, 5, 1, 4);
        engine.determineFirstPlayer();
        assertEquals(List.of(Colour.GREEN, Colour.YELLOW, Colour.BLUE, Colour.RED),
                engine.getPlayers().stream().map(Player::getColour).toList());
    }

    @Test @DisplayName("clockwise order wraps round the table whoever starts")
    void clockwiseOrderForEveryStarter() {
        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE), orderAfter(6, 1, 2, 3));
        assertEquals(List.of(Colour.YELLOW, Colour.BLUE, Colour.RED, Colour.GREEN), orderAfter(1, 2, 6, 3));
        assertEquals(List.of(Colour.BLUE, Colour.RED, Colour.GREEN, Colour.YELLOW), orderAfter(1, 2, 3, 6));
    }

    private List<Colour> orderAfter(int... rolls) {
        GameEngine engine = buildWith(rolls);
        engine.determineFirstPlayer();
        return engine.getPlayers().stream().map(Player::getColour).toList();
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

    // ── roll-off: ties ───────────────────────────────────────────────────────

    @Test @DisplayName("players tied for the highest roll roll again; the others do not")
    void tiedHighestRollersRollAgain() {
        // RED=4, GREEN=4, YELLOW=2, BLUE=4 -> RED, GREEN, BLUE roll again: RED=3, GREEN=6, BLUE=5 -> GREEN starts
        GameEngine engine = buildWith(4, 4, 2, 4, 3, 6, 5);
        Player first = engine.determineFirstPlayer();
        assertEquals(Colour.GREEN, first.getColour());
        List<RollOffEvent> ro = rollOffEvents();
        assertEquals(7, ro.size(), "four first rolls plus three re-rolls");
        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.BLUE),
                ro.subList(4, 7).stream().map(e -> e.player().getColour()).toList());
        assertEquals(List.of(Colour.GREEN, Colour.YELLOW, Colour.BLUE, Colour.RED),
                engine.getPlayers().stream().map(Player::getColour).toList());
    }

    @Test @DisplayName("if everyone ties, everyone rolls again")
    void everyoneTiesEveryoneRollsAgain() {
        GameEngine engine = buildWith(3, 3, 3, 3, 1, 2, 3, 6);
        Player first = engine.determineFirstPlayer();
        assertEquals(Colour.BLUE, first.getColour());
        assertEquals(8, rollOffEvents().size());
    }

    @Test @DisplayName("seed 55 (all four roll the same first time) still ends with one starter and a clockwise order")
    void seed55AllEqualRollsAgain() {
        GameEngine engine = new GameBuilder()
                .withRandom(new SeededRandomProvider(55))
                .withEventBus(bus)
                .withPlayers(4)
                .build();
        Player first = engine.determineFirstPlayer();

        List<RollOffEvent> ro = rollOffEvents();
        assertTrue(ro.size() > 4, "a tie forces another round of rolls");
        assertTrue(ro.subList(0, 4).stream().allMatch(e -> e.roll() == ro.get(0).roll()),
                "the first four rolls of seed 55 are all equal");
        List<Colour> order = engine.getPlayers().stream().map(Player::getColour).toList();
        assertEquals(first.getColour(), order.get(0));
        for (int i = 0; i < order.size(); i++) {
            Colour next = Colour.values()[(order.get(i).ordinal() + 1) % Colour.values().length];
            assertEquals(next, order.get((i + 1) % order.size()), "play passes clockwise");
        }
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

    @Test @DisplayName("seed 55: after the tie is re-rolled, TurnOrderEvent still reports a roll for every player")
    void seed55RollResultsCoverEveryPlayer() {
        GameEngine engine = new GameBuilder()
                .withRandom(new SeededRandomProvider(55))
                .withEventBus(bus)
                .withPlayers(4)
                .build();
        engine.determineFirstPlayer();
        TurnOrderEvent toe = turnOrderEvent();
        assertNotNull(toe);
        assertEquals(4, toe.rollResults().size());
        assertTrue(toe.rollResults().containsKey(toe.first().getColour()));
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
