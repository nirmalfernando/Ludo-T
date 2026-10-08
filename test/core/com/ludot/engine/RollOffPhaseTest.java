package com.ludot.engine;

import com.ludot.dice.FixedRandomProvider;
import com.ludot.events.EventBus;
import com.ludot.events.GameEvent;
import com.ludot.events.flow.GameStartEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.factory.GameBuilder;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The opening roll-off as a phase: contenders roll one at a time, in seat order, so a person can press Roll. */
@DisplayName("Opening roll-off as a phase (spec 1.1)")
class RollOffPhaseTest {

    private final List<GameEvent> events = new ArrayList<>();

    private GameEngine engine(int... dice) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        return new GameBuilder().withRandom(new FixedRandomProvider().addDice(dice)).withEventBus(bus).withPlayers(4).build();
    }

    private static String summary(List<GameEvent> events) {
        StringBuilder text = new StringBuilder();
        for (GameEvent event : events) {
            if (event instanceof GameStartEvent) {
                text.append("start;");
            } else if (event instanceof RollOffEvent e) {
                text.append(e.player().getColour()).append('=').append(e.roll()).append(';');
            } else if (event instanceof TurnOrderEvent e) {
                text.append("order:");
                e.order().forEach(p -> text.append(p.getColour().name().charAt(0)));
                text.append(';');
            }
        }
        return text.toString();
    }

    private static void rollOffByHand(GameEngine engine) {
        while (engine.getPhase() == GamePhase.ROLL_OFF) {
            engine.rollOff(engine.getCurrentPlayer().getColour());
        }
    }

    @Test @DisplayName("beginRollOff announces the game and waits: no die has been rolled yet")
    void beginRollOffWaitsForTheFirstRoller() {
        GameEngine engine = engine(3, 5, 1, 4);
        engine.beginRollOff();
        assertEquals(GamePhase.ROLL_OFF, engine.getPhase());
        assertEquals(Colour.RED, engine.getCurrentPlayer().getColour());
        assertEquals("start;", summary(events));
    }

    @Test @DisplayName("rolling by hand gives exactly the events and the first player that start() gives")
    void phasedRollOffEqualsTheAutomaticOne() {
        GameEngine automatic = engine(3, 5, 1, 4);
        automatic.start();
        String expected = summary(events);
        Colour expectedFirst = automatic.getCurrentPlayer().getColour();
        events.clear();

        GameEngine byHand = engine(3, 5, 1, 4);
        byHand.beginRollOff();
        rollOffByHand(byHand);

        assertEquals(expected, summary(events));
        assertEquals(expectedFirst, byHand.getCurrentPlayer().getColour());
        assertEquals(GamePhase.AWAITING_ROLL, byHand.getPhase());
        assertEquals(Colour.GREEN, byHand.getCurrentPlayer().getColour(), "5 is the highest roll");
    }

    @Test @DisplayName("a tie for the highest roll is rolled again by the tied players only, still one at a time")
    void tiedPlayersRollAgainInSeatOrder() {
        GameEngine engine = engine(4, 4, 2, 4, 3, 6, 5);
        engine.beginRollOff();
        for (Colour expected : List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE)) {
            assertEquals(expected, engine.getCurrentPlayer().getColour());
            engine.rollOff(expected);
        }
        assertEquals(GamePhase.ROLL_OFF, engine.getPhase(), "three players tied on 4");
        for (Colour expected : List.of(Colour.RED, Colour.GREEN, Colour.BLUE)) {
            assertEquals(expected, engine.getCurrentPlayer().getColour(), "Yellow is out of the re-roll");
            engine.rollOff(expected);
        }
        assertEquals(GamePhase.AWAITING_ROLL, engine.getPhase());
        assertEquals(Colour.GREEN, engine.getCurrentPlayer().getColour());
        assertEquals("start;RED=4;GREEN=4;YELLOW=2;BLUE=4;RED=3;GREEN=6;BLUE=5;order:GYBR;", summary(events));
    }

    @Test @DisplayName("every roll-off roll advances the turn number, so a late or repeated roll can be told from the current one")
    void eachRollOffRollIsAStep() {
        GameEngine engine = engine(3, 5, 1, 4);
        engine.beginRollOff();
        assertEquals(0, engine.getTurnNumber());
        engine.rollOff(Colour.RED);
        assertEquals(1, engine.getTurnNumber());
        engine.rollOff(Colour.GREEN);
        engine.rollOff(Colour.YELLOW);
        engine.rollOff(Colour.BLUE);
        assertEquals(4, engine.getTurnNumber());
    }

    @Test @DisplayName("the seat order is not rotated until the roll-off is over")
    void playersAreRotatedOnlyAtTheEnd() {
        GameEngine engine = engine(3, 5, 1, 4);
        engine.beginRollOff();
        engine.rollOff(Colour.RED);
        engine.rollOff(Colour.GREEN);
        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE), colours(engine.getPlayers()));
        engine.rollOff(Colour.YELLOW);
        engine.rollOff(Colour.BLUE);
        assertEquals(List.of(Colour.GREEN, Colour.YELLOW, Colour.BLUE, Colour.RED), colours(engine.getPlayers()));
    }

    @Test @DisplayName("only the contender whose turn it is may roll, and only during the roll-off")
    void wrongRollerOrPhaseIsRefused() {
        GameEngine engine = engine(3, 5, 1, 4);
        assertThrows(IllegalStateException.class, () -> engine.rollOff(Colour.RED), "not begun");
        engine.beginRollOff();
        assertThrows(IllegalStateException.class, () -> engine.rollOff(Colour.BLUE), "Red is next");
        assertThrows(IllegalStateException.class, () -> engine.roll(Colour.RED), "a normal roll is not a roll-off");
        rollOffByHand(engine);
        assertThrows(IllegalStateException.class, () -> engine.rollOff(Colour.GREEN), "the roll-off is over");
    }

    @Test @DisplayName("start() still leaves the game awaiting the winner's first roll")
    void startIsStillAutomatic() {
        GameEngine engine = engine(2, 6, 3, 1);
        engine.start();
        assertEquals(GamePhase.AWAITING_ROLL, engine.getPhase());
        assertEquals(Colour.GREEN, engine.getCurrentPlayer().getColour());
        assertEquals(4, engine.getTurnNumber());
    }

    private static List<Colour> colours(List<Player> players) {
        return players.stream().map(Player::getColour).toList();
    }
}
