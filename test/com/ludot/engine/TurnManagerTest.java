package com.ludot.engine;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.events.*;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.factory.*;
import com.ludot.model.*;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("TurnManager")
class TurnManagerTest {
    private TurnManager tm;
    private List<GameEvent> published;
    private List<Player> players;
    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        published = new ArrayList<>();
        EventBus bus = new EventBus();
        bus.subscribe(published::add);
        FixedRandomProvider random = new FixedRandomProvider().addDice(3,3,3,3,3);
        RuleEngine rules = RuleEngineFactory.createLudoTRuleEngine(random);
        MysteryManager mm = new MysteryManager(random, Board.getInstance());
        MoveExecutor me = new MoveExecutor(Board.getInstance(), rules, mm, bus);
        players = PlayerFactory.createAll();
        tm = new TurnManager(new HashMap<>(), rules, me, bus, random, players);
    }
    @AfterEach void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("executeTurn publishes DiceRollEvent") void publishesDiceRoll() {
        tm.executeTurn(players.get(0));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof DiceRollEvent));
    }
    @Test @DisplayName("nextPlayer rotates through players") void nextPlayerRotates() {
        Player second = tm.nextPlayer();
        assertEquals(players.get(1), second);
        Player third = tm.nextPlayer();
        assertEquals(players.get(2), third);
    }
    @Test @DisplayName("incrementRound increments count") void incrementsRound() {
        tm.incrementRound();
        assertEquals(1, tm.getRoundCount());
        tm.incrementRound();
        assertEquals(2, tm.getRoundCount());
    }
}