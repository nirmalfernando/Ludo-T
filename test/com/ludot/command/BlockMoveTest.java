package com.ludot.command;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("BlockMove")
class BlockMoveTest {
    @Test @DisplayName("execute moves all block pieces to target cell") void movesAllPieces() {}
    @Test @DisplayName("source cell is empty after move") void sourceCellEmptied() {}
    @Test @DisplayName("all pieces share the new position") void allPiecesSamePosition() {}
}