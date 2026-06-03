package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.dice.SeededRandomProvider;
import com.ludot.engine.GameEngine;
import com.ludot.engine.MoveExecutor;
import com.ludot.engine.MysteryManager;
import com.ludot.engine.TurnManager;
import com.ludot.events.ConsoleOutputListener;
import com.ludot.events.EventBus;
import com.ludot.events.OutputFormatter;
import com.ludot.model.Board;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.List;

// Builder — constructs and wires a fully playable GameEngine from a seed and player count.
public class GameBuilder {

    private long seed = System.currentTimeMillis();
    private int playerCount = 4;
    private RandomProvider customRandom = null;
    private EventBus customEventBus = null;

    public GameBuilder withSeed(long seed) {
        this.seed = seed;
        return this;
    }

    public GameBuilder withPlayers(int playerCount) {
        this.playerCount = playerCount;
        return this;
    }

    public GameBuilder withRandom(RandomProvider random) {
        this.customRandom = random;
        return this;
    }

    public GameBuilder withEventBus(EventBus eventBus) {
        this.customEventBus = eventBus;
        return this;
    }

    public GameEngine build() {
        RandomProvider random = customRandom != null ? customRandom : new SeededRandomProvider(seed);

        // reset ensures no piece/mystery state bleeds from a previous build() call
        Board.resetInstance();
        Board board = Board.getInstance();
        CellFactory.initializeBoard(board);

        List<Player> players = PlayerFactory.createAll().subList(0, playerCount);

        EventBus eventBus = customEventBus != null ? customEventBus : new EventBus();
        if (customEventBus == null) {
            eventBus.subscribe(new ConsoleOutputListener(new OutputFormatter()));
        }

        RuleEngine ruleEngine = RuleEngineFactory.createLudoTRuleEngine(random);

        MysteryManager mysteryManager = new MysteryManager(random, board);
        MoveExecutor moveExecutor = new MoveExecutor(board, ruleEngine, mysteryManager, eventBus, players);

        TurnManager turnManager = new TurnManager(ruleEngine, moveExecutor, eventBus, random, players);

        return new GameEngine(board, players, turnManager, moveExecutor,
                mysteryManager, ruleEngine, eventBus, random);
    }
}
