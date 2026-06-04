package com.ludot.engine;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.events.EventBus;
import com.ludot.events.GameEvent;
import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.model.state.OnBoardState;
import com.ludot.factory.CellFactory;
import com.ludot.factory.PlayerFactory;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
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
        tm = new TurnManager(rules, me, bus, random, players);
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
    @Test @DisplayName("handleTripleSix publishes TripleSixEvent")
    void tripleSixPublishesEvent() {
        tm.handleTripleSix(players.get(0));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof TripleSixEvent),
                "TripleSixEvent must be published when triple-six fires");
    }

    @Test @DisplayName("handleTripleSix breaks an active block and publishes BlockBreakEvent")
    void tripleSixBreaksBlock() {
        Player red = players.get(0);
        Cell cell20 = Board.getInstance().getCell(20);
        Piece p1 = red.getPieces().get(0);
        Piece p2 = red.getPieces().get(1);
        // place two RED pieces on cell 20 to form a block
        cell20.addPiece(p1); p1.setPosition(cell20); p1.setState(new OnBoardState()); p1.setDirection(Direction.CLOCKWISE);
        cell20.addPiece(p2); p2.setPosition(cell20); p2.setState(new OnBoardState()); p2.setDirection(Direction.CLOCKWISE);

        tm.handleTripleSix(red);

        assertTrue(published.stream().anyMatch(ev -> ev instanceof BlockBreakEvent),
                "BlockBreakEvent must be published when the block is dissolved");
    }

    @Test @DisplayName("handleBonusRoll executes an additional turn and publishes a DiceRollEvent")
    void bonusRollExecutesTurn() {
        long before = published.stream().filter(ev -> ev instanceof DiceRollEvent).count();
        tm.handleBonusRoll(players.get(0));
        long after = published.stream().filter(ev -> ev instanceof DiceRollEvent).count();
        assertEquals(before + 1, after, "Bonus roll must trigger exactly one additional DiceRollEvent");
    }

    @Test @DisplayName("incrementRound increments count") void incrementsRound() {
        tm.incrementRound();
        assertEquals(1, tm.getRoundCount());
        tm.incrementRound();
        assertEquals(2, tm.getRoundCount());
    }
}