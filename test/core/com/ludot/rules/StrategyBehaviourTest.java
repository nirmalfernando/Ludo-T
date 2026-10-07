package com.ludot.rules;

import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.GameState;
import com.ludot.model.Piece;
import com.ludot.strategy.GreenStrategy;
import com.ludot.strategy.PlayerStrategy;
import com.ludot.strategy.RedStrategy;
import com.ludot.strategy.YellowStrategy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The behaviour of the Red, Green and Yellow players, bullet by bullet as in the specification (section 2.1). */
@DisplayName("Player behaviour (spec 2.1.1 to 2.1.3)")
class StrategyBehaviourTest {

    private static Move choose(RuleScenario game, PlayerStrategy strategy, Colour colour, int roll) {
        for (Colour c : Colour.values()) {
            game.playerOf(c);
        }
        List<Move> moves = game.movesFor(colour, roll);
        List<Piece> pieces = game.playerOf(colour).getPieces();
        return strategy.decideMoves(new GameState(game.players, game.board, 0), roll, pieces, moves);
    }

    private static Piece pieceOf(Move move) {
        if (move instanceof CaptureMove capture) {
            return capture.getAttacker();
        }
        if (move instanceof MoveOnBoard onBoard) {
            return onBoard.getPiece();
        }
        if (move instanceof MoveFromBase fromBase) {
            return fromBase.getPiece();
        }
        throw new AssertionError("unexpected move " + move);
    }

    // ------------------------------------------------------------------ Red (2.1.1)

    @Test @DisplayName("Red: with several captures possible, it takes the victim closest to its home")
    void redCapturesTheVictimClosestToItsHome() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE);
        Piece r2 = game.piece("R2", Colour.RED, 20, Direction.CLOCKWISE);
        game.piece("B1", Colour.BLUE, 12, Direction.CLOCKWISE); // 25 cells from its approach cell (37)
        game.piece("B2", Colour.BLUE, 22, Direction.CLOCKWISE); // 15 cells from its approach cell
        Move chosen = choose(game, new RedStrategy(), Colour.RED, 2);
        assertInstanceOf(CaptureMove.class, chosen);
        assertSame(r2, pieceOf(chosen));
    }

    @Test @DisplayName("Red keeps one piece on the path, but brings another out on a 6 when it cannot capture")
    void redBringsASecondPieceOutOnlyWhenItCannotCapture() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE);
        game.inBase("R2", Colour.RED);
        assertInstanceOf(MoveFromBase.class, choose(game, new RedStrategy(), Colour.RED, 6));
    }

    @Test @DisplayName("Red prefers a capture by six cells to bringing a piece out")
    void redCapturesInsteadOfLeavingBase() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE);
        game.inBase("R2", Colour.RED);
        game.piece("B1", Colour.BLUE, 16, Direction.CLOCKWISE);
        assertInstanceOf(CaptureMove.class, choose(game, new RedStrategy(), Colour.RED, 6));
    }

    @Test @DisplayName("Red avoids creating a block when another move is available")
    void redAvoidsBlocks() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("R1", Colour.RED, 10, Direction.CLOCKWISE); // +4 would land on R2
        Piece r2 = game.piece("R2", Colour.RED, 14, Direction.CLOCKWISE);
        game.piece("B1", Colour.BLUE, 15, Direction.CLOCKWISE); // the nearest opponent tempts it towards the block
        Move chosen = choose(game, new RedStrategy(), Colour.RED, 4);
        assertSame(r2, pieceOf(chosen));
    }

    // ------------------------------------------------------------------ Green (2.1.2)

    @Test @DisplayName("Green: a six takes a piece out of base...")
    void greenEmptiesItsBase() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("G1", Colour.GREEN, 20, Direction.CLOCKWISE);
        game.inBase("G2", Colour.GREEN);
        assertInstanceOf(MoveFromBase.class, choose(game, new GreenStrategy(), Colour.GREEN, 6));
    }

    @Test @DisplayName("...unless moving six cells creates a block")
    void greenPrefersToFormABlock() {
        RuleScenario game = RuleScenario.ludoT();
        Piece g1 = game.piece("G1", Colour.GREEN, 10, Direction.CLOCKWISE);
        game.piece("G2", Colour.GREEN, 16, Direction.CLOCKWISE);
        game.inBase("G3", Colour.GREEN);
        Move chosen = choose(game, new GreenStrategy(), Colour.GREEN, 6);
        assertSame(g1, pieceOf(chosen));
    }

    @Test @DisplayName("Green moves forward with the block move (T-4)")
    void greenMovesItsBlock() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("G1", Colour.GREEN, 10, Direction.CLOCKWISE);
        game.piece("G2", Colour.GREEN, 10, Direction.CLOCKWISE);
        game.piece("G3", Colour.GREEN, 30, Direction.CLOCKWISE);
        assertInstanceOf(BlockMove.class, choose(game, new GreenStrategy(), Colour.GREEN, 2));
    }

    @Test @DisplayName("Green moves its other pieces before it breaks a block")
    void greenKeepsItsBlockWhileOthersCanMove() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("G1", Colour.GREEN, 10, Direction.CLOCKWISE);
        game.piece("G2", Colour.GREEN, 10, Direction.CLOCKWISE);
        game.piece("G3", Colour.GREEN, 10, Direction.CLOCKWISE); // a roll of 2 cannot move a block of three
        Piece g4 = game.piece("G4", Colour.GREEN, 30, Direction.CLOCKWISE);
        assertSame(g4, pieceOf(choose(game, new GreenStrategy(), Colour.GREEN, 2)));
    }

    @Test @DisplayName("Green breaks a block only when nothing else can play the roll")
    void greenBreaksABlockAsALastResort() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("G1", Colour.GREEN, 10, Direction.CLOCKWISE);
        game.piece("G2", Colour.GREEN, 10, Direction.CLOCKWISE);
        game.piece("G3", Colour.GREEN, 10, Direction.CLOCKWISE);
        Move chosen = choose(game, new GreenStrategy(), Colour.GREEN, 2);
        assertInstanceOf(MoveOnBoard.class, chosen);
        assertEquals(10, pieceOf(chosen).getPosition().getId());
    }

    @Test @DisplayName("Green does not capture more than it needs for the home straight")
    void greenIgnoresCapturesItDoesNotNeed() {
        RuleScenario game = RuleScenario.ludoT();
        Piece g1 = game.piece("G1", Colour.GREEN, 1, Direction.CLOCKWISE);
        g1.incrementCaptureCount(); // already has the capture it needs
        game.piece("G2", Colour.GREEN, 30, Direction.CLOCKWISE);
        game.piece("B1", Colour.BLUE, 3, Direction.CLOCKWISE);
        assertNotSame(CaptureMove.class, choose(game, new GreenStrategy(), Colour.GREEN, 2).getClass());
    }

    @Test @DisplayName("Green does capture when the attacker still needs a capture")
    void greenCapturesWhenItNeedsTo() {
        RuleScenario game = RuleScenario.ludoT();
        game.piece("G1", Colour.GREEN, 1, Direction.CLOCKWISE);
        game.piece("G2", Colour.GREEN, 30, Direction.CLOCKWISE);
        game.piece("B1", Colour.BLUE, 3, Direction.CLOCKWISE);
        assertInstanceOf(CaptureMove.class, choose(game, new GreenStrategy(), Colour.GREEN, 2));
    }

    // ------------------------------------------------------------------ Yellow (2.1.3)

    @Test @DisplayName("Yellow: a six takes a piece out of base")
    void yellowEmptiesItsBase() {
        RuleScenario game = RuleScenario.ludoT();
        game.inBase("Y1", Colour.YELLOW);
        game.piece("Y2", Colour.YELLOW, 20, Direction.CLOCKWISE);
        assertInstanceOf(MoveFromBase.class, choose(game, new YellowStrategy(), Colour.YELLOW, 6));
    }

    @Test @DisplayName("Yellow lets a piece that still needs a capture capture, before anything else")
    void yellowCapturesWithAPieceThatNeedsIt() {
        RuleScenario game = RuleScenario.ludoT();
        Piece y1 = game.inStraight("Y1", Colour.YELLOW, 1);
        y1.incrementCaptureCount();
        Piece y2 = game.piece("Y2", Colour.YELLOW, 5, Direction.CLOCKWISE); // has no capture yet
        game.piece("B1", Colour.BLUE, 7, Direction.CLOCKWISE);
        Move chosen = choose(game, new YellowStrategy(), Colour.YELLOW, 2);
        assertInstanceOf(CaptureMove.class, chosen);
        assertSame(y2, pieceOf(chosen));
    }

    @Test @DisplayName("Yellow does not capture with a piece that has its capture already")
    void yellowIgnoresCapturesItDoesNotNeed() {
        RuleScenario game = RuleScenario.ludoT();
        Piece y2 = game.piece("Y2", Colour.YELLOW, 5, Direction.CLOCKWISE);
        y2.incrementCaptureCount();
        Piece y3 = game.piece("Y3", Colour.YELLOW, 20, Direction.CLOCKWISE);
        y3.incrementCaptureCount();
        game.piece("B1", Colour.BLUE, 7, Direction.CLOCKWISE);
        assertSame(y3, pieceOf(choose(game, new YellowStrategy(), Colour.YELLOW, 2)));
    }

    @Test @DisplayName("With no capture to make, Yellow moves the piece closest to its home")
    void yellowMovesThePieceClosestToHome() {
        RuleScenario game = RuleScenario.ludoT();
        Piece y2 = game.piece("Y2", Colour.YELLOW, 5, Direction.CLOCKWISE);
        Piece y3 = game.piece("Y3", Colour.YELLOW, 40, Direction.CLOCKWISE);
        y2.incrementCaptureCount();
        y3.incrementCaptureCount();
        assertSame(y3, pieceOf(choose(game, new YellowStrategy(), Colour.YELLOW, 3)));
    }

    @Test @DisplayName("A piece in the home straight is closer to home than any piece on the ring")
    void yellowFinishesTheHomeStraightFirst() {
        RuleScenario game = RuleScenario.ludoT();
        Piece y1 = game.inStraight("Y1", Colour.YELLOW, 2);
        Piece y2 = game.piece("Y2", Colour.YELLOW, 45, Direction.CLOCKWISE);
        y1.incrementCaptureCount();
        y2.incrementCaptureCount();
        assertSame(y1, pieceOf(choose(game, new YellowStrategy(), Colour.YELLOW, 1)));
    }
}
