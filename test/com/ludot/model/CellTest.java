package com.ludot.model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Cell")
class CellTest {
    private Cell cell;
    @BeforeEach void setUp() { cell = new Cell(0, CellType.STANDARD, null); }
    @Test @DisplayName("new cell is not occupied") void notOccupiedInitially() {
        assertFalse(cell.isOccupied());
    }
    @Test @DisplayName("addPiece marks cell as occupied") void addPieceOccupies() {
        cell.addPiece(new Piece("R1", Colour.RED));
        assertTrue(cell.isOccupied());
    }
    @Test @DisplayName("removePiece empties cell") void removePieceEmpties() {
        Piece p = new Piece("R1", Colour.RED);
        cell.addPiece(p);
        cell.removePiece(p);
        assertFalse(cell.isOccupied());
    }
    @Test @DisplayName("hasBlock true for 2 same-colour pieces") void hasBlockSameColour() {
        cell.addPiece(new Piece("R1", Colour.RED));
        cell.addPiece(new Piece("R2", Colour.RED));
        assertTrue(cell.hasBlock());
    }
    @Test @DisplayName("hasBlock false for mixed colours") void noBlockMixedColours() {
        cell.addPiece(new Piece("R1", Colour.RED));
        cell.addPiece(new Piece("B1", Colour.BLUE));
        assertFalse(cell.hasBlock());
    }
    @Test @DisplayName("getPieces returns unmodifiable view") void getPiecesUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
            () -> cell.getPieces().add(new Piece("R1", Colour.RED)));
    }
}