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
import com.ludot.GameConstants;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
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
    private final MysteryManager mysteryManager;
    private final RuleEngine ruleEngine;
    private final EventBus eventBus;
    private final RandomProvider random;
    private static final int MAX_ROLL_OFFS = 50; // a guard against dice that tie for ever (a rigged test source)

    private GamePhase phase = GamePhase.NOT_STARTED;
    private int currentIndex;   // seat in play order whose turn it is
    private int stepNumber;     // rolls + moves applied so far

    /** Constructs a GameEngine with all collaborators wired in by the caller (GameBuilder). */
    public GameEngine(Board board, List<Player> players, TurnManager turnManager,
                      MysteryManager mysteryManager,
                      RuleEngine ruleEngine, EventBus eventBus, RandomProvider random) {
        this.board = board;
        this.players = players;
        this.turnManager = turnManager;
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

    /** The board this game is played on. */
    public Board getBoard() {
        return board;
    }

    /** Completed rounds so far. */
    public int getRoundCount() {
        return turnManager.getRoundCount();
    }

    /** The mystery cell currently on the board, or null when none is active. */
    public Cell getMysteryCell() {
        return mysteryManager.getCurrentCell();
    }

    /** Rounds the active mystery cell has left. */
    public int getMysteryRoundsLeft() {
        return mysteryManager.getRoundsLeft();
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

    private int idleRounds;
    private String lastSignature = "";

    private void beginTurn() {
        turnManager.beginTurn(getCurrentPlayer());
        phase = GamePhase.AWAITING_ROLL;
    }

    // called when a player's turn (bonus rolls included) is fully over
    private void completeTurn() {
        boolean roundOver = currentIndex == players.size() - 1;
        boolean won = checkWinCondition();
        boolean stalemate = !won && roundOver && isStalemate();
        if (won || roundOver) {
            endRound();
        }
        if (won || stalemate) {
            Player winner = won ? players.stream().filter(Player::hasWon).findFirst().orElse(players.get(0))
                    : leader();
            eventBus.publish(new WinEvent(winner, turnManager.getRoundCount()));
            phase = GamePhase.FINISHED;
            return;
        }
        currentIndex = roundOver ? 0 : currentIndex + 1;
        beginTurn();
    }

    // Opposing blockades can end up jammed against each other so that no piece of any colour can ever move again (T-3
    // forbids jumping a blockade). After STALEMATE_ROUNDS rounds without a single piece moving the game is adjudicated
    // in favour of the player furthest ahead; this is documented in docs/RULES_INTERPRETATION.md.
    private boolean isStalemate() {
        String signature = players.stream().flatMap(player -> player.getPieces().stream())
                .map(piece -> (piece.getPosition() == null ? "-" : piece.getPosition().getId())
                        + piece.getState().getStateName())
                .collect(java.util.stream.Collectors.joining(","));
        idleRounds = signature.equals(lastSignature) ? idleRounds + 1 : 0;
        lastSignature = signature;
        return idleRounds >= GameConstants.STALEMATE_ROUNDS;
    }

    // the player with most pieces home, then most captures
    private Player leader() {
        return players.stream().max(java.util.Comparator
                .comparingInt((Player player) -> player.getPiecesAtHome().size())
                .thenComparingInt(player -> player.getPieces().stream().mapToInt(Piece::getCaptureCount).sum()))
                .orElse(players.get(0));
    }

    /**
     * Conducts the roll-off (PDF, section 1.1): everyone rolls and the highest roller plays first; players who tie for
     * the highest roll roll again. Play then passes clockwise, which is the order of the colours R, G, Y, B.
     */
    public Player determineFirstPlayer() {
        Map<Colour, Integer> rollResults = new HashMap<>();
        List<Player> contenders = new ArrayList<>(players);
        for (int attempt = 0; contenders.size() > 1 && attempt < MAX_ROLL_OFFS; attempt++) {
            int highest = 0;
            for (Player player : contenders) {
                int roll = random.rollDice();
                eventBus.publish(new RollOffEvent(player, roll));
                rollResults.put(player.getColour(), roll);
                highest = Math.max(highest, roll);
            }
            final int best = highest;
            contenders.removeIf(player -> rollResults.get(player.getColour()) != best);
        }
        Player first = contenders.get(0);
        int at = players.indexOf(first);
        List<Player> clockwise = new ArrayList<>(players.subList(at, players.size()));
        clockwise.addAll(players.subList(0, at));
        players.clear();
        players.addAll(clockwise);
        eventBus.publish(new TurnOrderEvent(first, List.copyOf(players), Map.copyOf(rollResults)));
        return first;
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

    // T-10: the cell appears after two rounds, stays four rounds, then reappears at another place
    private void tickMysteryLifecycle(int round) {
        if (!ruleEngine.supportsMysteryCells()) {
            return; // Classic has no mystery cells
        }
        if (!mysteryManager.isActive()) {
            spawnMysteryCell(round);
            return;
        }
        mysteryManager.tick().ifPresentOrElse(expired -> {
            eventBus.publish(new MysteryExpireEvent(expired));
            spawnMysteryCell(round);
        }, () -> eventBus.publish(new MysteryTickEvent(mysteryManager.getCurrentCell(), mysteryManager.getRoundsLeft())));
    }

    private void spawnMysteryCell(int round) {
        mysteryManager.spawnMystery(round);
        if (mysteryManager.isActive()) {
            eventBus.publish(new MysterySpawnEvent(mysteryManager.getCurrentCell(), mysteryManager.getLifespan()));
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
