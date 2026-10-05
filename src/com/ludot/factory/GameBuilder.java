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

public class GameBuilder {

    private long seed = System.currentTimeMillis();
    private int playerCount = 4;
    private RandomProvider customRandom = null;
    private EventBus customEventBus = null;
    private RuleEngineFactory ruleEngineFactory = new LudoTRuleEngineFactory();

    
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

    
    public GameBuilder withRuleEngineFactory(RuleEngineFactory factory) {
        this.ruleEngineFactory = factory;
        return this;
    }

    public GameEngine build() {
        RandomProvider random = customRandom != null ? customRandom : new SeededRandomProvider(seed);

        // every build() gets its own Board, so games never share piece or mystery state
        Board board = new Board();
        CellFactory.initializeBoard(board);

        List<Player> players = PlayerFactory.createAll(board).subList(0, playerCount);

        EventBus eventBus = customEventBus != null ? customEventBus : new EventBus();
        if (customEventBus == null) {
            eventBus.subscribe(new ConsoleOutputListener(new OutputFormatter()));
        }

        RuleEngine ruleEngine = ruleEngineFactory.create(random);
        ruleEngine.setEventBus(eventBus);

        MysteryManager mysteryManager = new MysteryManager(random, board);
        MoveExecutor moveExecutor = new MoveExecutor(board, ruleEngine, mysteryManager, eventBus, players);

        TurnManager turnManager = new TurnManager(ruleEngine, moveExecutor, eventBus, random, players, board);

        return new GameEngine(board, players, turnManager, moveExecutor,
                mysteryManager, ruleEngine, eventBus, random);
    }
}
