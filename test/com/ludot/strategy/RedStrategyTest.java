package com.ludot.strategy;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.SkipMove;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("RedStrategy — aggressive")
class RedStrategyTest {
    private final RedStrategy strategy = new RedStrategy();
    private final Player player = new Player(Colour.RED, List.of(), (gs,r,ps,m)->m.get(0));
    @Test @DisplayName("prefers CaptureMove over other moves") void prefersCaptureMove() {
        Piece att = new Piece("R1", Colour.RED); Piece vic = new Piece("B1", Colour.BLUE);
        Cell c1 = new Cell(4, CellType.STANDARD, null);
        Cell c2 = new Cell(5, CellType.STANDARD, null);
        c1.addPiece(att); att.setPosition(c1);
        c2.addPiece(vic); vic.setPosition(c2);
        Move skip    = new SkipMove(player);
        Move capture = new CaptureMove(att, vic, c2, 1);
        assertInstanceOf(CaptureMove.class, strategy.decideMoves(null, 1, List.of(), List.of(skip, capture)));
    }
    @Test @DisplayName("falls back to first move when no capture") void fallbackToFirst() {
        Move skip = new SkipMove(player);
        assertSame(skip, strategy.decideMoves(null, 1, List.of(), List.of(skip)));
    }
}