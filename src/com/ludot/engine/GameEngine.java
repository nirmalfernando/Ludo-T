package com.ludot.engine;

import com.ludot.dice.RandomProvider;
import com.ludot.events.EventBus;
import com.ludot.events.flow.GameStartEvent;
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

/**
 * Orchestrates the full game loop including turn sequencing, win detection, and event publishing.
 * Implements the Mediator pattern, coordinating TurnManager, MoveExecutor, MysteryManager, and the EventBus.
 */
public class GameEngine {

    private final Board board;
    private final List<Player> players;
    private final TurnManager turnManager;
    private final MoveExecutor moveExecutor;
    private final MysteryManager mysteryManager;
    private final RuleEngine ruleEngine;
    private final EventBus eventBus;
    private final RandomProvider random;

    /** Constructs a GameEngine with all collaborators wired in by the caller (GameBuilder). */
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

    /** Starts and drives the game loop until a winner is found, then publishes a WinEvent. */
    public void run() {
        eventBus.publish(new GameStartEvent(List.copyOf(players)));
        determineFirstPlayer();
        while (!checkWinCondition()) {
            playRound();
        }
        Player winner = players.stream().filter(Player::hasWon).findFirst().orElse(players.get(0));
        eventBus.publish(new WinEvent(winner, turnManager.getRoundCount()));
    }

    /** Conducts the roll-off to determine play order; highest roll goes first, ties broken alphabetically. */
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

    private void playRound() {
        boolean winMidRound = false;
        for (Player player : players) {
            if (!player.hasWon()) {
                turnManager.executeTurn(player);
            }
            if (checkWinCondition()) {
                winMidRound = true;
                break;
            }
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

        // always publish round-end status, even when someone won mid-round
        eventBus.publish(new RoundEndEvent(round, List.copyOf(players)));

        // exit after publishing round status so the final board state is visible
        if (winMidRound) return;
    }

    /** Returns true if any player has moved all pieces to the home position. */
    public boolean checkWinCondition() {
        return players.stream().anyMatch(Player::hasWon);
    }

    /** Returns an unmodifiable snapshot of the current player list in play order. */
    public List<Player> getPlayers() { return List.copyOf(players); }

    private List<Piece> getAllPieces() {
        List<Piece> all = new ArrayList<>();
        for (Player p : players) {
            all.addAll(p.getPieces());
        }
        return all;
    }
}
