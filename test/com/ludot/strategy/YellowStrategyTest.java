package com.ludot.strategy;
import com.ludot.command.*;
import com.ludot.model.*;
import com.ludot.model.state.InHomeStraightState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("YellowStrategy — progress")
class YellowStrategyTest {
    private final YellowStrategy strategy = new YellowStrategy();
    private final Player player = new Player(Colour.YELLOW, List.of(), (gs,r,ps,m)->m.get(0));
    @Test @DisplayName("prefers move for piece in home straight") void prefersHomeStraight() {
        Piece onBoard  = new Piece("Y1", Colour.YELLOW);
        Piece inHome   = new Piece("Y2", Colour.YELLOW);
        inHome.setState(new InHomeStraightState());
        Cell c = new Cell(0, CellType.HOME_STRAIGHT, Colour.YELLOW);
        c.addPiece(inHome); inHome.setPosition(c);
        Move normalMove = new MoveOnBoard(onBoard, 3);
        Move homeMove   = new MoveOnBoard(inHome,  1);
        Move chosen = strategy.decideMoves(null, 1, List.of(), List.of(normalMove, homeMove));
        assertSame(homeMove, chosen);
    }
    @Test @DisplayName("falls back to first move otherwise") void fallbackToFirst() {
        Move skip = new SkipMove(player);
        assertSame(skip, strategy.decideMoves(null, 1, List.of(), List.of(skip)));
    }
}