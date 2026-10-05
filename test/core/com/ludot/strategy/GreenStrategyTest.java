package com.ludot.strategy;

import com.ludot.model.Board;
import com.ludot.command.BlockMove;
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
@DisplayName("GreenStrategy — defensive")
class GreenStrategyTest {

    private Board board;
    private final GreenStrategy strategy = new GreenStrategy();
    private final Player player = new Player(Colour.RED, List.of(), (gs,r,ps,m)->m.get(0));
    @Test @DisplayName("prefers BlockMove over all") void prefersBlockMove() {
        Piece p1 = new Piece("R1", Colour.RED, board); Piece p2 = new Piece("R2", Colour.RED, board);
        Cell src = new Cell(0, CellType.STANDARD, null); Cell tgt = new Cell(3, CellType.STANDARD, null);
        src.addPiece(p1); src.addPiece(p2); p1.setPosition(src); p2.setPosition(src);
        Move skip  = new SkipMove(player);
        Move block = new BlockMove(List.of(p1, p2), tgt, 3, 6);
        assertInstanceOf(BlockMove.class, strategy.decideMoves(null, 3, List.of(), List.of(skip, block)));
    }
    @Test @DisplayName("prefers CaptureMove when no block") void captureWhenNoBlock() {
        Piece att = new Piece("R1", Colour.RED, board); Piece vic = new Piece("B1", Colour.BLUE, board);
        Cell c1 = new Cell(4, CellType.STANDARD, null); Cell c2 = new Cell(5, CellType.STANDARD, null);
        c1.addPiece(att); att.setPosition(c1); c2.addPiece(vic); vic.setPosition(c2);
        Move skip    = new SkipMove(player);
        Move capture = new CaptureMove(att, vic, c2, 1);
        assertInstanceOf(CaptureMove.class, strategy.decideMoves(null, 1, List.of(), List.of(skip, capture)));
    }
    @Test @DisplayName("falls back to first move otherwise") void fallbackToFirst() {
        Move skip = new SkipMove(player);
        assertSame(skip, strategy.decideMoves(null, 1, List.of(), List.of(skip)));
    }
}