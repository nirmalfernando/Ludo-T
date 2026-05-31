package com.ludot.engine;

import java.util.List;
import java.util.Map;
import com.ludot.dice.RandomProvider;
import com.ludot.events.EventBus;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;

// Template Method managing turn sequencing, bonus rolls, and triple-six handling per player.

public class TurnManager {

    private int currentPlayerIndex;
    private int roundCount;
    private final Map<Colour, Integer> consecutiveSixes;
    private final RuleEngine ruleEngine;
    private final MoveExecutor moveExecutor;
    private final EventBus eventBus;
    private final RandomProvider random;
    private final List<Player> players;

    public TurnManager(Map<Colour, Integer> consecutiveSixes, RuleEngine ruleEngine,
                       MoveExecutor moveExecutor, EventBus eventBus,
                       RandomProvider random, List<Player> players) {
        this.consecutiveSixes = consecutiveSixes;
        this.ruleEngine = ruleEngine;
        this.moveExecutor = moveExecutor;
        this.eventBus = eventBus;
        this.random = random;
        this.players = players;
        this.currentPlayerIndex = 0;
        this.roundCount = 0;
    }

    public void executeTurn(Player player) {
    }

    public int rollDice() {
        return 0;
    }

    public void handleTripleSix(Player player) {
    }

    public void handleBonusRoll(Player player) {
    }

    public Player nextPlayer() {
        return null;
    }

    public int getRoundCount() {
        return roundCount;
    }
}
