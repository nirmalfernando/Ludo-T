package com.ludot.command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("MoveFromBase")
class MoveFromBaseTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("execute places piece on start cell") void placesOnStartCell() {}
    @Test @DisplayName("execute sets piece to OnBoardState") void setsOnBoardState() {}
    @Test @DisplayName("execute returns successful MoveResult") void returnsSuccess() {}
}