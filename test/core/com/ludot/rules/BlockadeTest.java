package com.ludot.rules;

import com.ludot.command.BlockCaptureMove;
import com.ludot.command.BlockMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.AtBaseState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// T-3 (blocks cannot be jumped), T-4 (block movement), T-8 (block against block) and Rule 6 for blocks
@DisplayName("Blockades (T-3, T-4, T-8)")
class BlockadeTest {

    private static void block(RuleScenario game, Colour colour, int cellId, int size) {
        for (int i = 1; i <= size; i++) {
            game.piece(colour.name().charAt(0) + "" + i, colour, cellId, Direction.CLOCKWISE);
        }
    }

    private static MoveOnBoard moveOf(List<Move> moves, Piece piece) {
        return (MoveOnBoard) moves.stream().filter(m -> m instanceof MoveOnBoard mob && mob.getPiece() == piece)
                .findFirst().orElseThrow(() -> new AssertionError(piece.getName() + " has no ordinary move in " + moves));
    }

    private static Cell finalCell(RuleScenario game, MoveOnBoard move) {
        game.executor().execute(move, game.playerOf(move.getPiece().getColour()));
        return move.getPiece().getPosition();
    }

    @Test @DisplayName("T-3 (the PDF's example): G1 on cell 0 rolls 6 with a block on cell 4: it may only go up to cell 3")
    void opponentStopsBeforeABlockThatIsMidPath() {
        RuleScenario game = RuleScenario.ludoT();
        Piece g1 = game.piece("G1", Colour.GREEN, 0, Direction.CLOCKWISE);
        block(game, Colour.RED, 4, 2);
        assertSame(game.cell(3), finalCell(game, moveOf(game.movesFor(Colour.GREEN, 6), g1)));
    }

    @Test @DisplayName("T-3: a roll that would land exactly on the block also stops on the cell before it")
    void rollLandingOnTheBlockStopsBefore() {
        RuleScenario game = RuleScenario.ludoT();
        Piece g1 = game.piece("G1", Colour.GREEN, 0, Direction.CLOCKWISE);
        block(game, Colour.RED, 4, 2);
        assertSame(game.cell(3), finalCell(game, moveOf(game.movesFor(Colour.GREEN, 4), g1)));
    }

    @Test @DisplayName("T-3: a roll that stops short of the block is an ordinary move")
    void rollShortOfTheBlockIsUnaffected() {
        RuleScenario game = RuleScenario.ludoT();
        Piece g1 = game.piece("G1", Colour.GREEN, 0, Direction.CLOCKWISE);
        block(game, Colour.RED, 4, 2);
        assertSame(game.cell(2), finalCell(game, moveOf(game.movesFor(Colour.GREEN, 2), g1)));
    }

    @Test @DisplayName("T-3: right next to a block no movement is possible at all")
    void nothingIsPossibleNextToABlock() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("G1", Colour.GREEN, 0, Direction.CLOCKWISE);
        block(game, Colour.RED, 1, 2);
        List<Move> moves = game.movesFor(Colour.GREEN, 5);
        assertEquals(1, moves.size());
        assertInstanceOf(SkipMove.class, moves.get(0));
    }

    @Test @DisplayName("T-3: a block does not hold up pieces of its own colour")
    void ownBlockIsNotAnObstacle() {
        RuleScenario game = RuleScenario.ludoT();
        Piece r1 = game.piece("R1", Colour.RED, 0, Direction.CLOCKWISE);
        game.piece("R2", Colour.RED, 3, Direction.CLOCKWISE);
        game.piece("R3", Colour.RED, 3, Direction.CLOCKWISE);
        assertSame(game.cell(5), finalCell(game, moveOf(game.movesFor(Colour.RED, 5), r1)));
    }

    @Test @DisplayName("T-4: a block of size n moves roll / n cells (rounded down); too small a roll cannot move it")
    void blockMovesRollOverSize() {
        for (int size = 2; size <= 4; size++) {
            for (int roll = 1; roll <= 6; roll++) {
                RuleScenario game = RuleScenario.ludoT();
                block(game, Colour.RED, 10, size);
                List<Move> moves = game.movesFor(Colour.RED, roll);
                int expected = roll / size;
                String label = "block of " + size + " rolling " + roll;
                if (expected == 0) {
                    // the block cannot move as one, but each piece may still leave it on its own (T-5)
                    assertTrue(moves.stream().noneMatch(m -> m instanceof BlockMove), label);
                    assertEquals(size, moves.size(), label);
                    assertTrue(moves.stream().allMatch(m -> m instanceof MoveOnBoard), label);
                } else {
                    BlockMove move = (BlockMove) moves.stream().filter(m -> m instanceof BlockMove).findFirst()
                            .orElseThrow(() -> new AssertionError(label + " has no block move"));
                    assertEquals(expected, move.getSteps(), label);
                    assertSame(game.cell(10 + expected), move.getTargetCell(), label);
                }
            }
        }
    }

    @Test @DisplayName("T-3: a block cannot jump an opposing block either: it stops on the cell before it")
    void blockStopsBeforeAnOpposingBlock() {
        RuleScenario game = RuleScenario.ludoT();
        block(game, Colour.RED, 10, 2);
        block(game, Colour.BLUE, 12, 2);
        BlockMove move = (BlockMove) game.movesFor(Colour.RED, 6).stream().filter(m -> m instanceof BlockMove)
                .findFirst().orElseThrow();
        assertSame(game.cell(11), move.getTargetCell());
        assertEquals(1, move.getSteps());
    }

    @Test @DisplayName("T-8: a block captures an opposing block of the SAME size; every attacker is credited exactly once")
    void equalSizedBlocksCapture() {
        RuleScenario game = RuleScenario.ludoT();
        block(game, Colour.RED, 10, 2);
        block(game, Colour.BLUE, 13, 2);
        List<Piece> blue = new ArrayList<>(game.cell(13).getPieces());
        BlockCaptureMove capture = (BlockCaptureMove) game.movesFor(Colour.RED, 6).stream()
                .filter(m -> m instanceof BlockCaptureMove).findFirst().orElseThrow();
        List<Piece> attackers = new ArrayList<>(capture.getAttackers());

        game.executor().execute(capture, game.playerOf(Colour.RED));

        for (Piece attacker : attackers) {
            assertSame(game.cell(13), attacker.getPosition());
            assertEquals(1, attacker.getCaptureCount(), attacker.getName() + " must be credited once (T-8)");
        }
        for (Piece victim : blue) {
            assertNull(victim.getPosition(), victim.getName() + " goes back to base");
            assertInstanceOf(AtBaseState.class, victim.getState());
            assertEquals(0, victim.getCaptureCount(), "T-9: all information is reset");
        }
    }

    @Test @DisplayName("T-8: blocks of different sizes cannot capture each other; the smaller one stops in front")
    void differentSizedBlocksDoNotCapture() {
        RuleScenario game = RuleScenario.ludoT();
        block(game, Colour.RED, 10, 2);
        block(game, Colour.BLUE, 13, 3);
        List<Move> moves = game.movesFor(Colour.RED, 6);
        assertTrue(moves.stream().noneMatch(m -> m instanceof BlockCaptureMove));
        BlockMove stopped = (BlockMove) moves.stream().filter(m -> m instanceof BlockMove).findFirst().orElseThrow();
        assertSame(game.cell(12), stopped.getTargetCell());
    }

    @Test @DisplayName("Rule 6: a block that lands on a lone opponent captures it instead of sharing the cell")
    void blockLandingOnALonePieceCapturesIt() {
        RuleScenario game = RuleScenario.ludoT();
        block(game, Colour.RED, 10, 2);
        Piece lone = game.piece("B1", Colour.BLUE, 13, Direction.CLOCKWISE);
        BlockCaptureMove capture = (BlockCaptureMove) game.movesFor(Colour.RED, 6).stream()
                .filter(m -> m instanceof BlockCaptureMove).findFirst().orElseThrow(
                        () -> new AssertionError("the block should capture the lone piece"));
        game.executor().execute(capture, game.playerOf(Colour.RED));
        assertNull(lone.getPosition());
        assertEquals(1, game.cell(13).getPieces().stream().map(Piece::getColour).distinct().count(),
                "the cell must not end up shared by two colours");
    }
}
