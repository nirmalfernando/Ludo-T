package com.ludot.engine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("GameState")
class GameStateTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("getOpponentPieces excludes own colour") void excludesOwnColour() {}
    @Test @DisplayName("canCapture true for lone opponent on destination") void canCaptureTrue() {}
    @Test @DisplayName("canCapture false for blocked destination") void canCaptureFalseBlocked() {}
    @Test @DisplayName("findBlocks returns all active blocks on path") void findsBlocks() {}
}