package com.ludot.rules;

import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.AtHomeState;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// rule 9 (land or pass the approach cell), Rule 10 (exact roll home), T-7 (a capture first) and T-1 (second pass)
@DisplayName("Entering the home straight (Rules 9 and 10, T-1, T-7)")
class HomeStraightEntryTest {

    // red's approach cell is 24 and its home straight continues from it: the cells after 24 are straight[0..4], 4 = Home
    private static MoveOnBoard onlyMove(List<Move> moves) {
        assertEquals(1, moves.size(), "expected exactly one move but got " + moves);
        return assertInstanceOf(MoveOnBoard.class, moves.get(0));
    }

    // plays the move and reports where the piece actually ended up (independent of how the engine plans routes)
    private static com.ludot.model.Cell finalCell(RuleScenario game, MoveOnBoard move) {
        game.executor().execute(move, game.playerOf(move.getPiece().getColour()));
        return move.getPiece().getPosition();
    }

    @Test @DisplayName("Rule 9: a piece that PASSES the approach cell carries on into the home straight")
    void passingTheApproachCellEntersTheHomeStraight() {
        RuleScenario game = RuleScenario.classic();
        game.piece("R1", Colour.RED, 22, Direction.CLOCKWISE);
        // 22 -> 23 -> 24 (approach) -> straight[0] -> straight[1] -> straight[2]
        assertSame(game.straight(Colour.RED, 2), finalCell(game, onlyMove(game.movesFor(Colour.RED, 5))));
    }

    @Test @DisplayName("Rule 9: landing exactly ON the approach cell keeps the piece there until its next move")
    void landingOnTheApproachCellDoesNotTeleport() {
        RuleScenario game = RuleScenario.classic();
        game.piece("R1", Colour.RED, 22, Direction.CLOCKWISE);
        assertSame(game.cell(24), finalCell(game, onlyMove(game.movesFor(Colour.RED, 2))));
    }

    @Test @DisplayName("Rule 9: from the approach cell the very next step is the first home-straight cell")
    void leavingTheApproachCellEntersTheStraight() {
        RuleScenario game = RuleScenario.classic();
        game.piece("R1", Colour.RED, 24, Direction.CLOCKWISE);
        assertSame(game.straight(Colour.RED, 2), finalCell(game, onlyMove(game.movesFor(Colour.RED, 3))));
    }

    @Test @DisplayName("executing a move that passes the approach cell leaves the piece in the home straight state")
    void executedMoveChangesState() {
        RuleScenario game = RuleScenario.classic();
        Piece piece = game.piece("R1", Colour.RED, 22, Direction.CLOCKWISE);
        game.executor().execute(onlyMove(game.movesFor(Colour.RED, 5)), game.playerOf(Colour.RED));
        assertInstanceOf(InHomeStraightState.class, piece.getState());
        assertSame(game.straight(Colour.RED, 2), piece.getPosition());
    }

    @Test @DisplayName("Rule 10: an overshoot of home is not allowed, and an exact roll reaches Home")
    void exactRollToReachHome() {
        RuleScenario game = RuleScenario.classic();
        Piece piece = game.piece("R1", Colour.RED, 22, Direction.CLOCKWISE);
        game.piece("R2", Colour.RED, 40, Direction.CLOCKWISE); // another piece so the roll is not skipped for lack of pieces
        piece.getPosition().removePiece(piece);
        game.straight(Colour.RED, 3).addPiece(piece);
        piece.setPosition(game.straight(Colour.RED, 3));
        piece.setState(new InHomeStraightState());

        List<Move> tooBig = game.movesFor(Colour.RED, 3);
        assertTrue(tooBig.stream().noneMatch(m -> m instanceof MoveOnBoard mob && mob.getPiece() == piece),
                "3 steps from straight[3] would overshoot Home");
        MoveOnBoard exact = (MoveOnBoard) game.movesFor(Colour.RED, 1).stream()
                .filter(m -> m instanceof MoveOnBoard mob && mob.getPiece() == piece).findFirst().orElseThrow();
        game.executor().execute(exact, game.playerOf(Colour.RED));
        assertInstanceOf(AtHomeState.class, piece.getState());
    }

    @Test @DisplayName("T-7: a LUDO-T piece with no capture does NOT turn in: it carries on round the board")
    void withoutACaptureThePieceStaysOnThePath() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("R1", Colour.RED, 22, Direction.CLOCKWISE);
        assertSame(game.cell(27), finalCell(game, onlyMove(game.movesFor(Colour.RED, 5))));
    }

    @Test @DisplayName("T-7: after one capture the same move turns into the home straight")
    void afterACaptureThePieceTurnsIn() {
        RuleScenario game = RuleScenario.ludoT();
        Piece piece = game.piece("R1", Colour.RED, 22, Direction.CLOCKWISE);
        piece.incrementCaptureCount();
        assertSame(game.straight(Colour.RED, 2), finalCell(game, onlyMove(game.movesFor(Colour.RED, 5))));
    }

    @Test @DisplayName("T-7: passing the approach cell without a capture announces that a capture is needed")
    void refusalIsAnnounced() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("R1", Colour.RED, 22, Direction.CLOCKWISE);
        game.executor().execute(onlyMove(game.movesFor(Colour.RED, 5)), game.playerOf(Colour.RED));
        assertTrue(game.events.stream().anyMatch(e -> e.getClass().getSimpleName().equals("HomeEntryRejectedEvent")));
    }

    @Test @DisplayName("T-1: a counter-clockwise piece may turn in only when it reaches the approach cell for the SECOND time")
    void counterClockwiseNeedsTheSecondPass() {
        RuleScenario game = RuleScenario.ludoT();
        Piece firstPass = game.piece("R1", Colour.RED, 26, Direction.COUNTER_CLOCKWISE);
        firstPass.incrementCaptureCount(); // T-7 is satisfied, so only the pass count decides
        // 26 -> 25 -> 24 (first arrival at the approach cell) -> 23 -> 22 -> 21 -> 20: it does not turn in
        assertSame(game.cell(20), finalCell(game, onlyMove(game.movesFor(Colour.RED, 6))));

        RuleScenario second = RuleScenario.ludoT();
        Piece secondPass = second.piece("R1", Colour.RED, 26, Direction.COUNTER_CLOCKWISE);
        secondPass.incrementCaptureCount();
        secondPass.incrementApproachPassCount(); // it has been past the approach cell once already
        // 26 -> 25 -> 24 (second arrival) -> straight[0] -> straight[1] -> straight[2]
        assertSame(second.straight(Colour.RED, 2), finalCell(second, onlyMove(second.movesFor(Colour.RED, 5))));
    }

    @Test @DisplayName("T-1: a counter-clockwise piece with a capture but only one pass lands on the approach cell and stays on the path")
    void counterClockwiseLandingWithOnePassDoesNotEnter() {
        RuleScenario game = RuleScenario.ludoT();
        Piece piece = game.piece("R1", Colour.RED, 26, Direction.COUNTER_CLOCKWISE);
        piece.incrementCaptureCount();
        MoveOnBoard move = onlyMove(game.movesFor(Colour.RED, 2));
        game.executor().execute(move, game.playerOf(Colour.RED));
        assertSame(game.cell(24), piece.getPosition());
        assertEquals(1, piece.getApproachPassCount());
        assertTrue(piece.getState() instanceof com.ludot.model.state.OnBoardState);
    }

    @Test @DisplayName("a capture on the way counts the same approach passes as an ordinary move (T-1)")
    void capturingMovesCountApproachPasses() {
        RuleScenario game = RuleScenario.ludoT();
        Piece piece = game.piece("R1", Colour.RED, 26, Direction.COUNTER_CLOCKWISE);
        game.piece("B1", Colour.BLUE, 22, Direction.CLOCKWISE);
        List<Move> moves = game.movesFor(Colour.RED, 4); // 25, 24 (pass), 23, 22: lands on the blue piece
        CaptureMove capture = (CaptureMove) moves.stream().filter(m -> m instanceof CaptureMove).findFirst().orElseThrow();
        game.executor().execute(capture, game.playerOf(Colour.RED));
        assertEquals(1, piece.getApproachPassCount());
    }

    @Test @DisplayName("two of a colour's pieces on one home-straight cell are not a blockade: both can still advance")
    void homeStraightStackIsNotABlock() {
        RuleScenario game = RuleScenario.ludoT();
        game.inStraight("R1", Colour.RED, 0);
        game.inStraight("R2", Colour.RED, 0);
        List<Move> moves = game.movesFor(Colour.RED, 1);
        assertEquals(2, moves.size());
        assertTrue(moves.stream().allMatch(m -> m instanceof MoveOnBoard));
        assertTrue(moves.stream().noneMatch(m -> m instanceof SkipMove));
    }
}
