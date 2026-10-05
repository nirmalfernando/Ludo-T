package com.ludot.engine;

import com.ludot.GameConstants;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
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
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.Cell;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages per-player turn sequencing, bonus rolls, and triple-six penalty handling.
 * Implements Rules T-2 (bonus roll on capture or six) and T-6 (triple-six forces block break).
 */
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

    /** Constructs a TurnManager with all rule and engine collaborators injected by the caller. */
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

    /** Executes a full turn for the given player, including roll, move selection, and bonus-roll handling. */
    public void executeTurn(Player player) {
        executeTurn(player, 0);
    }

    private void executeTurn(Player player, int bonusDepth) {
        if (bonusDepth == 0) {
            eventBus.publish(new TurnStartEvent(player));
            turnCount++;
        }
        int roll = rollDice();
        eventBus.publish(new DiceRollEvent(player, roll));

        // BaseDiceRule tracks consecutive sixes; returns false on triple-six, true on bonus
        boolean bonusRoll = ruleEngine.getDiceRule().handleRoll(player, roll);

        if (roll == GameConstants.LEAVE_BASE_ROLL && !bonusRoll) {
            handleTripleSix(player);
            return;
        }

        GameState gameState = new GameState(players, board, turnCount);

        // O15: snapshot pieces in briefing before validation so we can detect beta escapes after
        List<Piece> inBriefingBefore = player.getPieces().stream()
                .filter(p -> p.getState() instanceof AtBriefingState)
                .collect(java.util.stream.Collectors.toList());

        List<Move> validMoves = ruleEngine.getValidMoves(player, roll);

        // publish BriefingEscapeEvent for any piece that escaped via 3 consecutive 3s
        for (Piece p : inBriefingBefore) {
            if (p.getState() instanceof AtBaseState) {
                eventBus.publish(new BriefingEscapeEvent(player, p));
            }
        }
        Move chosen = player.getStrategy().decideMoves(gameState, roll, player.getPieces(), validMoves);

        if (chosen instanceof SkipMove) {
            eventBus.publish(new SkipTurnEvent(player));
        }

        MoveResult result = moveExecutor.execute(chosen, player);

        // T-2: capture grants a bonus roll; rolling 6 (non-triple) also grants one
        // depth guard prevents runaway recursion on a very lucky streak
        if ((result.grantsBonusRoll() || bonusRoll) && bonusDepth < GameConstants.MAX_BONUS_DEPTH) {
            executeTurn(player, bonusDepth + 1);
        }
    }

    /** Rolls the dice once and returns the result via the injected RandomProvider. */
    public int rollDice() {
        return random.rollDice();
    }

    /** Handles a triple-six penalty by forcing a block break and moving each freed piece six cells (Rule T-6). */
    public void handleTripleSix(Player player) {
        boolean hasBlock = board.getStandardPath().stream()
                .anyMatch(cell -> cell.hasBlock()
                              && !cell.getPieces().isEmpty()
                              && cell.getPieces().get(0).getColour() == player.getColour());
        eventBus.publish(new TripleSixEvent(player, hasBlock));
        for (Cell cell : board.getStandardPath()) {
            if (cell.hasBlock() && cell.getPieces().get(0).getColour() == player.getColour()) {
                List<Piece> block = new ArrayList<>(cell.getPieces());
                ruleEngine.getBlockRule().forceBreakBlock(block);
                eventBus.publish(new BlockBreakEvent(block, cell));
                // T-6: each piece moves 6 units in its restored direction after the break
                for (Piece piece : block) {
                    if (piece.getDirection() != null) {
                        moveExecutor.execute(new MoveOnBoard(piece, GameConstants.LEAVE_BASE_ROLL), player);
                    }
                }
                break; // a player can only have one block at a time
            }
        }
    }

    /** Grants the given player an additional full turn as a bonus roll reward (Rule T-2). */
    public void handleBonusRoll(Player player) {
        executeTurn(player);
    }

    /** Advances the internal index and returns the next player in the rotation. */
    public Player nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        return players.get(currentPlayerIndex);
    }

    /** Increments the round counter after all players have taken their turns. */
    public void incrementRound() {
        roundCount++;
    }

    /** Returns the total number of completed rounds so far. */
    public int getRoundCount() {
        return roundCount;
    }
}
