package com.ludot.engine;

import com.ludot.GameConstants;
import com.ludot.command.Move;
import com.ludot.command.MoveResult;
import com.ludot.command.SkipMove;
import com.ludot.dice.RandomProvider;
import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.EventBus;
import com.ludot.events.flow.BriefingEscapeEvent;
import com.ludot.events.flow.TurnStartEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.model.Board;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.Cell;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
import java.util.List;

// implements Rules T-2 (bonus roll on capture or six) and T-6 (triple-six forces block break)
public class TurnManager {

    private int currentPlayerIndex;
    private int roundCount;
    private int turnCount; // increments on every real (non-bonus) turn; used by BlueStrategy for cycling
    private final RuleEngine ruleEngine;
    private final MoveExecutor moveExecutor;
    private final EventBus eventBus;
    private final RandomProvider random;
    private final List<Player> players;
    private final Board board;
    private Player activePlayer;        // null between turns
    private int bonusDepth;
    private boolean pendingBonusRoll;   // the last roll was a non-triple six
    private boolean awaitingMove;
    private List<Move> pendingMoves = List.of();

    public TurnManager(RuleEngine ruleEngine, MoveExecutor moveExecutor, EventBus eventBus,
                       RandomProvider random, List<Player> players, Board board) {
        this.ruleEngine = ruleEngine;
        this.moveExecutor = moveExecutor;
        this.eventBus = eventBus;
        this.random = random;
        this.players = players;
        this.board = board;
        this.currentPlayerIndex = 0;
        this.roundCount = 0;
    }

    public void executeTurn(Player player) {
        beginTurn(player);
        boolean turnOver = false;
        while (!turnOver) {
            RollResult rolled = roll();
            if (rolled.turnOver()) {
                return;
            }
            Move chosen = player.getStrategy().decideMoves(
                    currentGameState(), rolled.roll(), player.getPieces(), rolled.validMoves());
            turnOver = applyMove(chosen).turnOver();
        }
    }

    public void beginTurn(Player player) {
        activePlayer = player;
        bonusDepth = 0;
        awaitingMove = false;
        ruleEngine.getDiceRule().startTurn(player);
        eventBus.publish(new TurnStartEvent(player));
        turnCount++;
    }

    // leaves the turn awaiting a move choice, unless a triple six ends the turn immediately
    public RollResult roll() {
        if (activePlayer == null || awaitingMove) {
            throw new IllegalStateException("No roll is expected right now");
        }
        Player player = activePlayer;
        int roll = rollDice();
        eventBus.publish(new DiceRollEvent(player, roll));

        // BaseDiceRule tracks consecutive sixes; returns false on triple-six, true on bonus
        boolean bonusRoll = ruleEngine.getDiceRule().handleRoll(player, roll);

        if (roll == GameConstants.LEAVE_BASE_ROLL && !bonusRoll) {
            handleTripleSix(player);
            activePlayer = null;
            return new RollResult(roll, List.of(), true);
        }

        // T-13: every roll counts towards the three 3s that free a piece from briefing; a freed piece is back in base
        for (Piece p : player.getPieces()) {
            if (p.getState() instanceof AtBriefingState briefing && briefing.registerRoll(p, roll)) {
                eventBus.publish(new BriefingEscapeEvent(player, p));
            }
        }
        List<Move> validMoves = ruleEngine.getValidMoves(player, roll);
        pendingMoves = List.copyOf(validMoves);
        pendingBonusRoll = bonusRoll;
        awaitingMove = true;
        return new RollResult(roll, pendingMoves, false);
    }

    public StepResult applyMove(Move chosen) {
        if (!awaitingMove) {
            throw new IllegalStateException("No move is expected right now");
        }
        if (!pendingMoves.contains(chosen)) {
            throw new IllegalArgumentException("Move is not one of the valid moves for this roll");
        }
        Player player = activePlayer;
        if (chosen instanceof SkipMove) {
            eventBus.publish(new SkipTurnEvent(player));
        }
        MoveResult result = moveExecutor.execute(chosen, player);

        // T-2: capture grants a bonus roll
        boolean rollAgain = (result.grantsBonusRoll() || pendingBonusRoll)
                && !(chosen instanceof SkipMove) && bonusDepth < GameConstants.MAX_BONUS_DEPTH;
        awaitingMove = false;
        pendingMoves = List.of();
        if (rollAgain) {
            bonusDepth++;
        } else {
            activePlayer = null;
        }
        return new StepResult(result, !rollAgain);
    }

    public boolean isAwaitingMove() {
        return awaitingMove;
    }

    public List<Move> getPendingMoves() {
        return pendingMoves;
    }

    public GameState currentGameState() {
        return new GameState(players, board, turnCount);
    }

    public int getTurnCount() {
        return turnCount;
    }

    public int rollDice() {
        return random.rollDice();
    }

    // handles a triple six (Rules 4 and T-6)
    public void handleTripleSix(Player player) {
        List<Piece> block = ruleEngine.supportsBlocks() ? findBlockOf(player) : List.of();
        eventBus.publish(new TripleSixEvent(player, !block.isEmpty()));
        if (block.isEmpty()) {
            return;
        }
        Cell cell = block.get(0).getPosition();
        ruleEngine.getBlockRule().forceBreakBlock(block); // every piece gets its original direction back (T-5)
        eventBus.publish(new BlockBreakEvent(block, cell));
        for (int freed = 1; freed < block.size(); freed++) {
            ruleEngine.planMove(block.get(freed), GameConstants.LEAVE_BASE_ROLL * freed)
                    .ifPresent(move -> moveExecutor.execute(move, player));
        }
    }

    // a player can only have one block at a time
    private List<Piece> findBlockOf(Player player) {
        for (Cell cell : board.getStandardPath()) {
            if (cell.hasBlock() && cell.getPieces().get(0).getColour() == player.getColour()) {
                return new ArrayList<>(cell.getPieces());
            }
        }
        return List.of();
    }

    // grants the given player an additional full turn as a bonus roll reward (Rule T-2)
    public void handleBonusRoll(Player player) {
        executeTurn(player);
    }

    public Player nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        return players.get(currentPlayerIndex);
    }

    public void incrementRound() {
        roundCount++;
    }

    public int getRoundCount() {
        return roundCount;
    }
}
