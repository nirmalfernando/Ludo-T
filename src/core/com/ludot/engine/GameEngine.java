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
    private int stepNumber;     // roll-off rolls + rolls + moves applied so far

    // the opening roll-off: who is still rolling, whose turn it is, and what was rolled so far
    private final List<Player> rollOffContenders = new ArrayList<>();
    private final Map<Colour, Integer> rollOffResults = new HashMap<>();
    private int rollOffCursor;
    private int rollOffHighest;
    private int rollOffRounds;

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

    public void start() {
        beginRollOff();
        determineFirstPlayer();
    }

    public void beginRollOff() {
        requirePhase(GamePhase.NOT_STARTED);
        eventBus.publish(new GameStartEvent(List.copyOf(players)));
        rollOffContenders.clear();
        rollOffContenders.addAll(players);
        rollOffResults.clear();
        rollOffCursor = 0;
        rollOffHighest = 0;
        rollOffRounds = 0;
        phase = GamePhase.ROLL_OFF;
    }

    // the dice are drawn in exactly the order the automatic roll-off draws them, so a seed gives the same game either way
    public int rollOff(Colour colour) {
        requirePhase(GamePhase.ROLL_OFF);
        requireCurrentPlayer(colour);
        Player player = rollOffContenders.get(rollOffCursor);
        int roll = random.rollDice();
        eventBus.publish(new RollOffEvent(player, roll));
        rollOffResults.put(player.getColour(), roll);
        rollOffHighest = Math.max(rollOffHighest, roll);
        stepNumber++;
        rollOffCursor++;
        if (rollOffCursor == rollOffContenders.size()) {
            endRollOffRound();
        }
        return roll;
    }

    private void endRollOffRound() {
        final int best = rollOffHighest;
        rollOffContenders.removeIf(player -> rollOffResults.get(player.getColour()) != best);
        rollOffRounds++;
        rollOffCursor = 0;
        rollOffHighest = 0;
        if (rollOffContenders.size() == 1 || rollOffRounds >= MAX_ROLL_OFFS) {
            announceFirstPlayer(rollOffContenders.get(0));
        }
    }

    // play passes clockwise from the winner, which is the order of the colours R, G, Y, B
    private void announceFirstPlayer(Player first) {
        int at = players.indexOf(first);
        List<Player> clockwise = new ArrayList<>(players.subList(at, players.size()));
        clockwise.addAll(players.subList(0, at));
        players.clear();
        players.addAll(clockwise);
        eventBus.publish(new TurnOrderEvent(first, List.copyOf(players), Map.copyOf(rollOffResults)));
        currentIndex = 0;
        beginTurn();
    }

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

    public GamePhase getPhase() {
        return phase;
    }

    public Player getCurrentPlayer() {
        return phase == GamePhase.ROLL_OFF ? rollOffContenders.get(rollOffCursor) : players.get(currentIndex);
    }

    public Board getBoard() {
        return board;
    }

    public int getRoundCount() {
        return turnManager.getRoundCount();
    }

    public Cell getMysteryCell() {
        return mysteryManager.getCurrentCell();
    }

    public int getMysteryRoundsLeft() {
        return mysteryManager.getRoundsLeft();
    }

    // requests quote it so stale ones can be rejected
    public int getTurnNumber() {
        return stepNumber;
    }

    // the valid moves offered by the last roll (empty unless awaiting a move)
    public List<Move> getPendingMoves() {
        return turnManager.getPendingMoves();
    }

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

    // opposing blockades can end up jammed against each other so that no piece of any colour can ever move again (T-3 forbids jumping a blockade)
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

    public Player determineFirstPlayer() {
        if (phase == GamePhase.NOT_STARTED) {
            beginRollOff();
        }
        requirePhase(GamePhase.ROLL_OFF);
        while (phase == GamePhase.ROLL_OFF) {
            rollOff(getCurrentPlayer().getColour());
        }
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

    public boolean checkWinCondition() {
        return players.stream().anyMatch(Player::hasWon);
    }

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
