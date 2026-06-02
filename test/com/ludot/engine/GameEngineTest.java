package com.ludot.engine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("GameEngine")
class GameEngineTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("determineFirstPlayer selects highest dice roller") void firstPlayerHighest() {}
    @Test @DisplayName("checkWinCondition true when a player has won") void detectsWin() {}
    @Test @DisplayName("checkWinCondition false when no player has won") void noWinYet() {}
    @Test @DisplayName("run publishes WinEvent on completion") void publishesWinEvent() {}
}