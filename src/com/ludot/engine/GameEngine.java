package com.ludot.engine;

import com.ludot.dice.RandomProvider;
import com.ludot.events.EventBus;
import com.ludot.events.mystery.MysteryExpireEvent;
import com.ludot.events.mystery.MysterySpawnEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
import java.util.List;

// Mediator orchestrating the full game loop: turn sequencing, win detection, and event publishing.
public class GameEngine {

    private final Board board;
    private final List<Player> players;
    private final TurnManager turnManager;
    private final MoveExecutor moveExecutor;
    private final MysteryManager mysteryManager;
    private final RuleEngine ruleEngine;
    private final EventBus eventBus;
    private final RandomProvider random;

    public GameEngine(Board board, List<Player> players, TurnManager turnManager,
                      MoveExecutor moveExecutor, MysteryManager mysteryManager,
                      RuleEngine ruleEngine, EventBus eventBus, RandomProvider random) {
        this.board = board;
        this.players = players;
        this.turnManager = turnManager;
        this.moveExecutor = moveExecutor;
        this.mysteryManager = mysteryManager;
        this.ruleEngine = ruleEngine;
        this.eventBus = eventBus;
        this.random = random;
    }

    public void run() {
        determineFirstPlayer();
        while (!checkWinCondition()) {
            playRound();
        }
        Player winner = players.stream().filter(Player::hasWon).findFirst().orElse(players.get(0));
        eventBus.publish(new WinEvent(winner, turnManager.getRoundCount()));
    }

    // each player rolls once; highest roll starts (ties broken by list order)
    public Player determineFirstPlayer() {
        int bestRoll = -1;
        int firstIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            int roll = random.rollDice();
            if (roll > bestRoll) {
                bestRoll = roll;
                firstIndex = i;
            }
        }
        // rotate players so the winner of the toss is at index 0
        List<Player> reordered = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            reordered.add(players.get((firstIndex + i) % players.size()));
        }
        players.clear();
        players.addAll(reordered);
        return players.get(0);
    }

    public void playRound() {
        for (Player player : players) {
            if (!player.hasWon()) {
                turnManager.executeTurn(player);
            }
            if (checkWinCondition()) return;
        }
        turnManager.incrementRound();
        int round = turnManager.getRoundCount();

        // mystery cell lifecycle
        Cell cellBeforeSpawn = mysteryManager.getCurrentCell();
        mysteryManager.spawnMystery(round);
        if (mysteryManager.isActive() && cellBeforeSpawn == null) {
            eventBus.publish(new MysterySpawnEvent(mysteryManager.getCurrentCell(), round));
        }
        Cell cellBeforeTick = mysteryManager.getCurrentCell();
        mysteryManager.tick();
        if (cellBeforeTick != null && !mysteryManager.isActive()) {
            eventBus.publish(new MysteryExpireEvent(cellBeforeTick));
        }

        // tick active piece effects (Energised/Sick countdown)
        ruleEngine.getEffectRule().tickAllEffects(getAllPieces());

        eventBus.publish(new RoundEndEvent(round));
    }

    public boolean checkWinCondition() {
        return players.stream().anyMatch(Player::hasWon);
    }

    private List<Piece> getAllPieces() {
        List<Piece> all = new ArrayList<>();
        for (Player p : players) {
            all.addAll(p.getPieces());
        }
        return all;
    }
}
