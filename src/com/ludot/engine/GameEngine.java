package com.ludot.engine;

import com.ludot.command.Move;
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
import com.ludot.model.GameState;
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
    private GamePhase phase = GamePhase.NOT_STARTED;
    private int currentIndex;   // seat in play order whose turn it is
    private int stepNumber;     // rolls + moves applied so far

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

    /** Starts the game, then lets every seat's strategy play it to the end; the winner is announced by a WinEvent. */
    public void run() {
        start();
        while (phase != GamePhase.FINISHED) {
            Player player = getCurrentPlayer();
            RollResult rolled = roll(player.getColour());
            if (phase == GamePhase.AWAITING_MOVE) {
                Move chosen = player.getStrategy().decideMoves(
                        currentGameState(), rolled.roll(), player.getPieces(), rolled.validMoves());
                applyMove(player.getColour(), rolled.validMoves().indexOf(chosen));
            }
        }
    }

    /** Publishes the start event, runs the roll-off and opens the first player's turn. */
    public void start() {
        requirePhase(GamePhase.NOT_STARTED);
        eventBus.publish(new GameStartEvent(List.copyOf(players)));
        determineFirstPlayer();
        currentIndex = 0;
        beginTurn();
    }

    /** Roll phase for the player whose turn it is. A triple six forfeits the turn and moves play on. */
    public RollResult roll(Colour colour) {
        requirePhase(GamePhase.AWAITING_ROLL);
        requireCurrentPlayer(colour);
        RollResult result = turnManager.roll();
        stepNumber++;
        if (result.turnOver()) {
            completeTurn();
        } else {
            phase = GamePhase.AWAITING_MOVE;
        }
        return result;
    }

    /** Move phase: applies the index-th valid move offered by the last roll. */
    public StepResult applyMove(Colour colour, int moveIndex) {
        requirePhase(GamePhase.AWAITING_MOVE);
        requireCurrentPlayer(colour);
        List<Move> offered = turnManager.getPendingMoves();
        if (moveIndex < 0 || moveIndex >= offered.size()) {
            throw new IllegalArgumentException("Move index out of range: " + moveIndex);
        }
        StepResult result = turnManager.applyMove(offered.get(moveIndex));
        stepNumber++;
        if (result.turnOver()) {
            completeTurn();
        } else {
            phase = GamePhase.AWAITING_ROLL;
        }
        return result;
    }

    /** The current phase of the turn cycle. */
    public GamePhase getPhase() {
        return phase;
    }

    /** The player who must act now (roll or choose a move). */
    public Player getCurrentPlayer() {
        return players.get(currentIndex);
    }

    /** Counts every roll and move applied so far; requests quote it so stale ones can be rejected. */
    public int getTurnNumber() {
        return stepNumber;
    }

    /** The valid moves offered by the last roll (empty unless awaiting a move). */
    public List<Move> getPendingMoves() {
        return turnManager.getPendingMoves();
    }

    /** The view of the game strategies decide from. */
    public GameState currentGameState() {
        return turnManager.currentGameState();
    }

    private void requirePhase(GamePhase expected) {
        if (phase != expected) {
            throw new IllegalStateException("Expected phase " + expected + " but game is in " + phase);
        }
    }

    private void requireCurrentPlayer(Colour colour) {
        if (getCurrentPlayer().getColour() != colour) {
            throw new IllegalStateException("It is not " + colour + "'s turn");
        }
    }

    private void beginTurn() {
        turnManager.beginTurn(getCurrentPlayer());
        phase = GamePhase.AWAITING_ROLL;
    }

    // called when a player's turn (bonus rolls included) is fully over
    private void completeTurn() {
        boolean won = checkWinCondition();
        boolean roundOver = currentIndex == players.size() - 1;
        if (won || roundOver) {
            endRound();
        }
        if (won) {
            Player winner = players.stream().filter(Player::hasWon).findFirst().orElse(players.get(0));
            eventBus.publish(new WinEvent(winner, turnManager.getRoundCount()));
            phase = GamePhase.FINISHED;
            return;
        }
        currentIndex = roundOver ? 0 : currentIndex + 1;
        beginTurn();
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

    private void endRound() {
        turnManager.incrementRound();
        int round = turnManager.getRoundCount();

        tickMysteryLifecycle(round);

        if (ruleEngine.getEffectRule() != null) {
            ruleEngine.getEffectRule().tickAllEffects(getAllPieces());
        }

        eventBus.publish(new RoundEndEvent(round, List.copyOf(players),
                mysteryManager.getCurrentCell(), mysteryManager.getRoundsLeft()));
    }

    // T-10: spawn on the first eligible round; do not tick on the same round the cell spawns.
    private void tickMysteryLifecycle(int round) {
        Cell cellBeforeSpawn = mysteryManager.getCurrentCell();
        mysteryManager.spawnMystery(round);
        boolean justSpawned = cellBeforeSpawn == null && mysteryManager.isActive();
        if (justSpawned) {
            eventBus.publish(new MysterySpawnEvent(mysteryManager.getCurrentCell(),
                    mysteryManager.getLifespan()));
            return;
        }
        Cell cellBeforeTick = mysteryManager.getCurrentCell();
        mysteryManager.tick();
        if (cellBeforeTick != null && !mysteryManager.isActive()) {
            eventBus.publish(new MysteryExpireEvent(cellBeforeTick));
        } else if (mysteryManager.isActive()) {
            eventBus.publish(new MysteryTickEvent(mysteryManager.getCurrentCell(),
                    mysteryManager.getRoundsLeft()));
        }
    }

    /** Returns true if any player has moved all pieces to the home position. */
    public boolean checkWinCondition() {
        return players.stream().anyMatch(Player::hasWon);
    }

    /** Returns an unmodifiable snapshot of the current player list in play order. */
    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    private List<Piece> getAllPieces() {
        List<Piece> all = new ArrayList<>();
        for (Player p : players) {
            all.addAll(p.getPieces());
        }
        return all;
    }
}
