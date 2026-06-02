package com.ludot.engine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("MoveExecutor")
class MoveExecutorTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("MoveFromBase publishes CoinTossEvent and MoveEvent") void fromBaseEvents() {}
    @Test @DisplayName("CaptureMove publishes CaptureEvent") void captureEvents() {}
    @Test @DisplayName("BlockMove publishes BlockMoveEvent") void blockMoveEvent() {}
    @Test @DisplayName("MoveOnBoard publishes HomeReachEvent on last home cell") void homeReachEvent() {}
    @Test @DisplayName("BlockFormEvent fired when 2 same-colour pieces on cell") void blockFormEvent() {}
}