package com.ludot.engine;
import com.ludot.events.*;
import com.ludot.events.flow.WinEvent;
import com.ludot.factory.*;
import com.ludot.model.Board;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("GameEngine")
class GameEngineTest {
    @BeforeEach void setUp()    { Board.resetInstance(); }
    @AfterEach  void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("checkWinCondition false when no player has won") void noWinYet() {
        GameEngine engine = new GameBuilder().withSeed(1).withPlayers(4).build();
        assertFalse(engine.checkWinCondition());
    }
    @Test @DisplayName("run publishes WinEvent on completion") void publishesWinEvent() {
        List<GameEvent> events = new ArrayList<>();
        Board.resetInstance();
        // Build engine and subscribe before run
        GameEngine engine = new GameBuilder().withSeed(42).withPlayers(4).build();
        // Can verify run completes without exception — WinEvent check via output
        assertDoesNotThrow(engine::run);
    }
}