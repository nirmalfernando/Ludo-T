package com.ludot.rules;

import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.engine.RollResult;
import com.ludot.engine.StepResult;
import com.ludot.engine.TurnManager;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.model.state.AtBaseState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rules 4 and 7 (an ignored roll passes the dice), Rule 6 on leaving base, T-5 (leaving a block) and T-6 (triple six). */
@DisplayName("Turn rules and block breaking (Rules 4, 6, 7, T-5, T-6)")
class TurnAndBlockRulesTest {

    private static TurnManager turns(RuleScenario game) {
        var executor = game.executor();
        return new TurnManager(game.engine, executor, game.bus, game.random, game.players, game.board);
    }

    @Test @DisplayName("Rule 7: a roll that cannot be played, even a six, is ignored and the dice pass on (no bonus roll)")
    void anIgnoredSixGivesNoBonusRoll() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("R1", Colour.RED, 0, Direction.CLOCKWISE);
        game.atHome("R2", Colour.RED);
        game.atHome("R3", Colour.RED);
        game.atHome("R4", Colour.RED);
        game.piece("B1", Colour.BLUE, 1, Direction.CLOCKWISE);
        game.piece("B2", Colour.BLUE, 1, Direction.CLOCKWISE); // a block right in front of R1: nothing can move
        game.random.addDice(6);
        TurnManager turns = turns(game);
        Player red = game.playerOf(Colour.RED);
        game.players.removeIf(p -> p.getColour() == Colour.RED);
        game.players.add(red);

        turns.beginTurn(red);
        RollResult rolled = turns.roll();
        assertEquals(1, rolled.validMoves().size());
        assertInstanceOf(SkipMove.class, rolled.validMoves().get(0));
        StepResult step = turns.applyMove(rolled.validMoves().get(0));

        assertTrue(step.turnOver(), "the turn must end: an ignored six gives no bonus roll");
    }

    @Test @DisplayName("Rule 6: arriving on the start cell where a lone opponent stands captures it")
    void leavingBaseCapturesALoneOpponent() {
        RuleScenario game = RuleScenario.ludoT();
        Piece red = game.inBase("R1", Colour.RED);
        Piece blue = game.piece("B1", Colour.BLUE, 26, Direction.CLOCKWISE); // red's start cell
        MoveFromBase move = (MoveFromBase) game.movesFor(Colour.RED, 6).stream()
                .filter(m -> m instanceof MoveFromBase).findFirst().orElseThrow();
        assertSame(blue, move.getVictim(), "the arrival must capture the lone opponent");

        game.executor().execute(move, game.playerOf(Colour.RED));

        assertSame(game.cell(26), red.getPosition());
        assertNull(blue.getPosition(), "the captured piece is back in its base");
        assertEquals(1, red.getCaptureCount());
        assertEquals(1, game.cell(26).getPieces().size(), "the start cell must not end up shared by two colours");
    }

    @Test @DisplayName("T-6: three sixes with a block of 3: one piece stays, the others leave, six and then twelve cells on")
    void tripleSixBreaksTheBlockAndBaresOnePiece() {
        RuleScenario game = RuleScenario.ludoT();
        Piece r1 = game.piece("R1", Colour.RED, 10, Direction.COUNTER_CLOCKWISE); // currently moving with the block
        Piece r2 = game.piece("R2", Colour.RED, 10, Direction.CLOCKWISE);
        Piece r3 = game.piece("R3", Colour.RED, 10, Direction.CLOCKWISE);
        for (Piece p : List.of(r1, r2, r3)) {
            p.setOriginalDirection(Direction.CLOCKWISE);
        }
        TurnManager turns = turns(game);

        turns.handleTripleSix(game.playerOf(Colour.RED));

        assertSame(game.cell(10), r1.getPosition(), "the piece that is left behind stays on the block's cell");
        assertSame(game.cell(16), r2.getPosition(), "the next piece moves six units in its original direction");
        assertSame(game.cell(22), r3.getPosition(), "the third moves six more, cumulatively");
        assertEquals(Direction.CLOCKWISE, r1.getDirection(), "T-5: everyone has their original direction back");
        assertTrue(game.events.stream().anyMatch(e -> e.getClass().getSimpleName().equals("BlockBreakEvent")));
    }

    @Test @DisplayName("T-6: the freed pieces cannot jump an opposing block on their way")
    void tripleSixMovesStopAtOpposingBlocks() {
        RuleScenario game = RuleScenario.ludoT();
        Piece r1 = game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE);
        Piece r2 = game.piece("R2", Colour.RED, 10, Direction.CLOCKWISE);
        r1.setOriginalDirection(Direction.CLOCKWISE);
        r2.setOriginalDirection(Direction.CLOCKWISE);
        game.piece("B1", Colour.BLUE, 13, Direction.CLOCKWISE);
        game.piece("B2", Colour.BLUE, 13, Direction.CLOCKWISE);
        TurnManager turns = turns(game);

        turns.handleTripleSix(game.playerOf(Colour.RED));

        assertSame(game.cell(12), r2.getPosition(), "stopped on the cell before the blue block");
    }

    @Test @DisplayName("T-6: three sixes with no block just forfeit the turn; nothing moves")
    void tripleSixWithoutABlockMovesNothing() {
        RuleScenario game = RuleScenario.ludoT();
        Piece r1 = game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE);
        Piece r2 = game.piece("R2", Colour.RED, 20, Direction.CLOCKWISE);
        TurnManager turns = turns(game);

        turns.handleTripleSix(game.playerOf(Colour.RED));

        assertSame(game.cell(10), r1.getPosition());
        assertSame(game.cell(20), r2.getPosition());
    }

    @Test @DisplayName("T-5: a piece may leave its block on its own, and goes back to its original direction")
    void aPieceLeavingABlockRegainsItsOriginalDirection() {
        RuleScenario game = RuleScenario.ludoT();
        Piece r1 = game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE); // moving with the block
        Piece r2 = game.piece("R2", Colour.RED, 10, Direction.CLOCKWISE);
        r1.setOriginalDirection(Direction.COUNTER_CLOCKWISE);
        r2.setOriginalDirection(Direction.CLOCKWISE);

        var moves = game.movesFor(Colour.RED, 3);
        MoveOnBoard leave = (MoveOnBoard) moves.stream().filter(m -> m instanceof MoveOnBoard mob && mob.getPiece() == r1)
                .findFirst().orElseThrow(() -> new AssertionError("R1 should be able to leave its block: " + moves));

        game.executor().execute(leave, game.playerOf(Colour.RED));

        assertSame(game.cell(7), r1.getPosition(), "it leaves in its original (counter-clockwise) direction");
        assertEquals(Direction.COUNTER_CLOCKWISE, r1.getDirection());
        assertEquals(Direction.CLOCKWISE, r2.getDirection(), "the piece left behind is on its own again");
        assertTrue(game.events.stream().anyMatch(e -> e.getClass().getSimpleName().equals("BlockBreakEvent")));
    }

    @Test @DisplayName("T-4: when a block moves, every member adopts the direction of the member furthest from home")
    void blockMembersAdoptTheBlockDirection() {
        RuleScenario game = RuleScenario.ludoT();
        Piece r1 = game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE);
        Piece r2 = game.piece("R2", Colour.RED, 10, Direction.COUNTER_CLOCKWISE);
        r1.setOriginalDirection(Direction.CLOCKWISE);
        r2.setOriginalDirection(Direction.COUNTER_CLOCKWISE);
        var block = game.movesFor(Colour.RED, 2).stream().filter(m -> m instanceof com.ludot.command.BlockMove)
                .findFirst().orElseThrow();

        game.executor().execute(block, game.playerOf(Colour.RED));

        assertEquals(r1.getDirection(), r2.getDirection(), "one block, one direction");
        assertEquals(Direction.CLOCKWISE, r1.getOriginalDirection(), "what they revert to is not overwritten");
        assertEquals(Direction.COUNTER_CLOCKWISE, r2.getOriginalDirection());
    }

    @Test @DisplayName("Rule 4: the count of consecutive sixes belongs to one turn")
    void sixStreakStartsAgainEachTurn() {
        RuleScenario game = RuleScenario.ludoT();
        Player red = game.playerOf(Colour.RED);
        game.engine.getDiceRule().handleRoll(red, 6);
        game.engine.getDiceRule().handleRoll(red, 6);
        game.engine.getDiceRule().startTurn(red);
        assertTrue(game.engine.getDiceRule().handleRoll(red, 6), "the first six of a new turn is a bonus, not a triple");
    }

    @Test @DisplayName("sanity: a piece in base is untouched when it cannot leave")
    void baseStaysBaseWithoutASix() {
        RuleScenario game = RuleScenario.ludoT();
        Piece r1 = game.inBase("R1", Colour.RED);
        assertInstanceOf(SkipMove.class, game.movesFor(Colour.RED, 3).get(0));
        assertInstanceOf(AtBaseState.class, r1.getState());
    }
}
