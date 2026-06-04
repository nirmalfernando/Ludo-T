package com.ludot.rules;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.factory.CellFactory;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.model.state.OnBoardState;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("RuleEngine")
class RuleEngineTest {
    private RuleEngine engine;
    private FixedRandomProvider random;
    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        random = new FixedRandomProvider();
        engine = RuleEngineFactory.createLudoTRuleEngine(random);
    }
    @AfterEach void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("getValidMoves returns SkipMove when no moves available") void skipMoveWhenNone() {
        Player player = new Player(Colour.RED, List.of(new Piece("R1", Colour.RED)),
            (gs,r,ps,m)->m.get(0));
        List<Move> moves = engine.getValidMoves(player, 3); // pieces at base, non-6 roll
        assertEquals(1, moves.size());
        assertInstanceOf(SkipMove.class, moves.get(0));
    }
    @Test @DisplayName("getValidMoves upgrades to CaptureMove for lone opponent") void upgradeToCapture() {
        Piece red = new Piece("R1", Colour.RED);
        Cell src = Board.getInstance().getCell(26);
        src.addPiece(red); red.setPosition(src);
        red.setState(new OnBoardState()); red.setDirection(Direction.CLOCKWISE);
        Piece blue = new Piece("B1", Colour.BLUE);
        Cell dest = Board.getInstance().getCell(29);
        dest.addPiece(blue); blue.setPosition(dest); blue.setState(new OnBoardState());
        Player player = new Player(Colour.RED, List.of(red), (gs,r,ps,m)->m.get(0));
        List<Move> moves = engine.getValidMoves(player, 3);
        assertTrue(moves.stream().anyMatch(m -> m instanceof CaptureMove));
    }
    @Test @DisplayName("getValidMoves returns partial MoveOnBoard (one before block) when destination is blocked")
    void blockedReturnsPartialMove() {
        // T-3: piece at 26 CW, block at 29, roll 3 → partial move to 28 (3-1=2 steps)
        Piece red = new Piece("R1", Colour.RED);
        Cell src = Board.getInstance().getCell(26);
        src.addPiece(red); red.setPosition(src);
        red.setState(new OnBoardState()); red.setDirection(Direction.CLOCKWISE);
        Piece b1 = new Piece("B1", Colour.BLUE);
        Piece b2 = new Piece("B2", Colour.BLUE);
        Cell dest = Board.getInstance().getCell(29);
        dest.addPiece(b1); dest.addPiece(b2);
        b1.setPosition(dest); b2.setPosition(dest);
        Player player = new Player(Colour.RED, List.of(red), (gs,r,ps,m)->m.get(0));
        List<Move> moves = engine.getValidMoves(player, 3);
        // piece should receive a partial MoveOnBoard (2 steps to cell 28), not a SkipMove
        assertTrue(moves.stream().anyMatch(m -> m instanceof MoveOnBoard),
                "Blocked piece must receive a partial move to the cell before the block");
        assertFalse(moves.stream().allMatch(m -> m instanceof SkipMove),
                "SkipMove must not be the only option when a partial move is available");
    }
    @Test @DisplayName("getEffectiveSteps delegates to piece effect") void effectiveStesDelegates() {
        Piece p = new Piece("R1", Colour.RED);
        p.setActiveEffect(new com.ludot.effects.EnergisedEffect(4));
        assertEquals(6, p.getEffectiveSteps(3));
    }
}