package com.ludot.engine;
import com.ludot.factory.CellFactory;
import com.ludot.model.*;
import com.ludot.model.state.OnBoardState;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("GameState")
class GameStateTest {
    private Board board;
    @BeforeEach void setUp()    { Board.resetInstance(); CellFactory.initializeBoard(Board.getInstance()); board = Board.getInstance(); }
    @AfterEach  void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("getOpponentPieces excludes own colour") void excludesOwnColour() {
        Player red  = new Player(Colour.RED,  List.of(new Piece("R1", Colour.RED)),  (gs,r,ps,m)->m.get(0));
        Player blue = new Player(Colour.BLUE, List.of(new Piece("B1", Colour.BLUE)), (gs,r,ps,m)->m.get(0));
        GameState state = new GameState(List.of(red, blue), board);
        List<Piece> opp = state.getOpponentPieces(Colour.RED);
        assertTrue(opp.stream().allMatch(p -> p.getColour() != Colour.RED));
        assertEquals(1, opp.size());
    }
    @Test @DisplayName("canCapture true for lone opponent on destination") void canCaptureTrue() {
        Piece red  = new Piece("R1", Colour.RED);
        Piece blue = new Piece("B1", Colour.BLUE);
        Cell src  = board.getCell(26); Cell dest = board.getCell(29);
        src.addPiece(red);   red.setPosition(src);   red.setDirection(Direction.CLOCKWISE);
        dest.addPiece(blue); blue.setPosition(dest);  blue.setState(new OnBoardState());
        red.setState(new OnBoardState());
        GameState state = new GameState(List.of(
            new Player(Colour.RED, List.of(red), (gs,r,ps,m)->m.get(0)),
            new Player(Colour.BLUE, List.of(blue), (gs,r,ps,m)->m.get(0))), board);
        assertTrue(state.canCapture(red, 3));
    }
    @Test @DisplayName("findBlocks returns all active blocks on path") void findsBlocks() {
        Cell cell = board.getCell(10);
        Piece p1 = new Piece("R1", Colour.RED); Piece p2 = new Piece("R2", Colour.RED);
        cell.addPiece(p1); cell.addPiece(p2); p1.setPosition(cell); p2.setPosition(cell);
        GameState state = new GameState(List.of(), board);
        assertEquals(1, state.findBlocks().size());
    }
}