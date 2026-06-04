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
import com.ludot.events.flow.TurnStartEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
import java.util.List;

// Manages turn sequencing, bonus rolls, and triple-six handling per player (T-2, T-6).
public class TurnManager {

    private int currentPlayerIndex;
    private int roundCount;
    private int turnCount; // increments on every real (non-bonus) turn; used by BlueStrategy for cycling
    private final RuleEngine ruleEngine;
    private final MoveExecutor moveExecutor;
    private final EventBus eventBus;
    private final RandomProvider random;
    private final List<Player> players;

    public TurnManager(RuleEngine ruleEngine, MoveExecutor moveExecutor, EventBus eventBus,
                       RandomProvider random, List<Player> players) {
        this.ruleEngine = ruleEngine;
        this.moveExecutor = moveExecutor;
        this.eventBus = eventBus;
        this.random = random;
        this.players = players;
        this.currentPlayerIndex = 0;
        this.roundCount = 0;
    }

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

        GameState gameState = new GameState(players, Board.getInstance(), turnCount);
        List<Move> validMoves = ruleEngine.getValidMoves(player, roll);
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

    public int rollDice() {
        return random.rollDice();
    }

    // T-6: triple-six forces a block break if the player has an active block
    public void handleTripleSix(Player player) {
        eventBus.publish(new TripleSixEvent(player));
        for (Cell cell : Board.getInstance().getStandardPath()) {
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

    // T-2: a bonus roll is a full new turn for the same player
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

    public int getRoundCount() { return roundCount; }
}
