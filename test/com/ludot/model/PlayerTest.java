package com.ludot.model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Player")
class PlayerTest {
    @Test @DisplayName("getPiecesAtBase includes AtBaseState and AtBriefingState") void piecesAtBase() {}
    @Test @DisplayName("getPiecesOnBoard includes OnBoardState and InHomeStraightState") void piecesOnBoard() {}
    @Test @DisplayName("getPiecesAtHome includes AtHomeState only") void piecesAtHome() {}
    @Test @DisplayName("hasWon when all pieces are home") void hasWonAllHome() {}
    @Test @DisplayName("hasWon is false when any piece not home") void notWonYet() {}
}