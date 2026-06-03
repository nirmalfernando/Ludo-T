package com.ludot.factory;

import com.ludot.dice.SeededRandomProvider;
import com.ludot.engine.GameEngine;
import com.ludot.engine.MoveExecutor;
import com.ludot.engine.MysteryManager;
import com.ludot.engine.TurnManager;
import com.ludot.events.ConsoleOutputListener;
import com.ludot.events.EventBus;
import com.ludot.events.OutputFormatter;
import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Builder — constructs and wires a fully playable GameEngine from a seed and player count.
public class GameBuilder {

    private long seed = System.currentTimeMillis();
    private int playerCount = 4;

    public GameBuilder withSeed(long seed) {
        this.seed = seed;
        return this;
    }

    public GameBuilder withPlayers(int playerCount) {
        this.playerCount = playerCount;
        return this;
    }

    public GameEngine build() {
        SeededRandomProvider random = new SeededRandomProvider(seed);

        // fresh board every build
        Board board = Board.getInstance();
        CellFactory.initializeBoard(board);

        List<Player> players = PlayerFactory.createAll().subList(0, playerCount);

        EventBus eventBus = new EventBus();
        eventBus.subscribe(new ConsoleOutputListener(new OutputFormatter()));

        RuleEngine ruleEngine = RuleEngineFactory.createLudoTRuleEngine(random);

        MysteryManager mysteryManager = new MysteryManager(random, board);
        MoveExecutor moveExecutor = new MoveExecutor(board, ruleEngine, mysteryManager, eventBus, players);

        Map<Colour, Integer> sixTracker = new HashMap<>();
        TurnManager turnManager = new TurnManager(sixTracker, ruleEngine,
                moveExecutor, eventBus, random, players);

        return new GameEngine(board, players, turnManager, moveExecutor,
                mysteryManager, ruleEngine, eventBus, random);
    }
}
