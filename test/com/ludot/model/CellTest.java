package com.ludot.model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Cell")
class CellTest {
    private Cell cell;
    @BeforeEach void setUp() { cell = new Cell(0, CellType.STANDARD, null); }
    @Test @DisplayName("new cell is not occupied") void notOccupiedInitially() {}
    @Test @DisplayName("addPiece marks cell as occupied") void addPieceOccupies() {}
    @Test @DisplayName("removePiece empties cell") void removePieceEmpties() {}
    @Test @DisplayName("hasBlock true for 2 same-colour pieces") void hasBlockSameColour() {}
    @Test @DisplayName("hasBlock false for mixed colours") void noBlockMixedColours() {}
    @Test @DisplayName("getPieces returns unmodifiable view") void getPiecesUnmodifiable() {}
}