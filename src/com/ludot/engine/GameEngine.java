package com.ludot.engine;

import java.util.List;
import com.ludot.dice.RandomProvider;
import com.ludot.events.EventBus;
import com.ludot.model.Board;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;

// Mediator orchestrating the full game loop: turn sequencing, win detection, and event publishing.

public class GameEngine {

    private final Board board;
    private final List<Player> players;
    private final TurnManager turnManager;
    private final MoveExecutor moveExecutor;
    private final RuleEngine ruleEngine;
    private final EventBus eventBus;
    private final RandomProvider random;

    public GameEngine(Board board, List<Player> players, TurnManager turnManager,
                      MoveExecutor moveExecutor, RuleEngine ruleEngine,
                      EventBus eventBus, RandomProvider random) {
        this.board = board;
        this.players = players;
        this.turnManager = turnManager;
        this.moveExecutor = moveExecutor;
        this.ruleEngine = ruleEngine;
        this.eventBus = eventBus;
        this.random = random;
    }

    public void run() {
    }

    public Player determineFirstPlayer() {
        return null;
    }

    public void playRound() {
    }

    public boolean checkWinCondition() {
        return false;
    }
}
