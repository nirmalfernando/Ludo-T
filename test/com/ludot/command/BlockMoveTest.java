package com.ludot.command;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BlockMove")
class BlockMoveTest {
    private List<Piece> block;
    private Cell source, target;
    @BeforeEach void setUp() {
        source = new Cell(0, CellType.STANDARD, null);
        target = new Cell(3, CellType.STANDARD, null);
        Piece p1 = new Piece("R1", Colour.RED);
        Piece p2 = new Piece("R2", Colour.RED);
        source.addPiece(p1); source.addPiece(p2);
        p1.setPosition(source); p2.setPosition(source);
        block = List.of(p1, p2);
    }
    @Test @DisplayName("execute moves all block pieces to target cell") void movesAllPieces() {
        new BlockMove(block, target, 3, 6).execute();
        assertEquals(2, target.getPieces().size());
    }
    @Test @DisplayName("source cell is empty after move") void sourceCellEmptied() {
        new BlockMove(block, target, 3, 6).execute();
        assertFalse(source.isOccupied());
    }
    @Test @DisplayName("all pieces share the new position") void allPiecesSamePosition() {
        new BlockMove(block, target, 3, 6).execute();
        for (Piece p : block) assertEquals(target, p.getPosition());
    }
}