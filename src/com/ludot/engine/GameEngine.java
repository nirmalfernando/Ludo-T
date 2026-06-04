package com.ludot.engine;

import com.ludot.dice.RandomProvider;
import com.ludot.events.EventBus;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.events.mystery.MysteryExpireEvent;
import com.ludot.events.mystery.MysterySpawnEvent;
import com.ludot.events.mystery.MysteryTickEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // each player rolls once; play order is highest roll first, lowest last (ties broken alphabetically)
    public Player determineFirstPlayer() {
        List<int[]> rolls = new ArrayList<>(); // [originalIndex, roll]
        Map<Colour, Integer> rollResults = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            int roll = random.rollDice();
            eventBus.publish(new RollOffEvent(players.get(i), roll));
            rolls.add(new int[]{i, roll});
            rollResults.put(players.get(i).getColour(), roll);
        }
        // descending by roll; ties broken alphabetically by colour name (e.g. BLUE < GREEN < RED < YELLOW)
        rolls.sort(Comparator.comparingInt((int[] r) -> r[1]).reversed()
                .thenComparing(r -> players.get(r[0]).getColour().name()));
        List<Player> reordered = new ArrayList<>();
        for (int[] r : rolls) {
            reordered.add(players.get(r[0]));
        }
        players.clear();
        players.addAll(reordered);
        eventBus.publish(new TurnOrderEvent(players.get(0), List.copyOf(players), Map.copyOf(rollResults)));
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
        boolean justSpawned = cellBeforeSpawn == null && mysteryManager.isActive();
        if (justSpawned) {
            eventBus.publish(new MysterySpawnEvent(mysteryManager.getCurrentCell(),
                    mysteryManager.getLifespan()));
        }

        // T-10: do not tick on the same round the cell spawns — the lifespan is 4 full rounds,
        // ticking on the spawn round would silently reduce it to 3.
        if (!justSpawned) {
            Cell cellBeforeTick = mysteryManager.getCurrentCell();
            mysteryManager.tick();
            if (cellBeforeTick != null && !mysteryManager.isActive()) {
                eventBus.publish(new MysteryExpireEvent(cellBeforeTick));
            } else if (mysteryManager.isActive()) {
                eventBus.publish(new MysteryTickEvent(mysteryManager.getCurrentCell(),
                        mysteryManager.getRoundsLeft()));
            }
        }

        // tick active piece effects (Energised/Sick countdown)
        ruleEngine.getEffectRule().tickAllEffects(getAllPieces());

        eventBus.publish(new RoundEndEvent(round, List.copyOf(players)));
    }

    public boolean checkWinCondition() {
        return players.stream().anyMatch(Player::hasWon);
    }

    public List<Player> getPlayers() { return List.copyOf(players); }

    private List<Piece> getAllPieces() {
        List<Piece> all = new ArrayList<>();
        for (Player p : players) {
            all.addAll(p.getPieces());
        }
        return all;
    }
}
