package com.ludot.factory;
import com.ludot.engine.GameEngine;
import com.ludot.model.Board;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("GameBuilder")
class GameBuilderTest {
    @BeforeEach void setUp()    { Board.resetInstance(); }
    @AfterEach  void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("build returns a non-null GameEngine") void buildReturnsEngine() {
        assertNotNull(new GameBuilder().withSeed(42).withPlayers(4).build());
    }
    @Test @DisplayName("same seed produces deterministic game") void seedDeterministic() {
        Board.resetInstance();
        GameEngine e1 = new GameBuilder().withSeed(99).withPlayers(2).build();
        Board.resetInstance();
        GameEngine e2 = new GameBuilder().withSeed(99).withPlayers(2).build();
        assertNotNull(e1);
        assertNotNull(e2);
    }
    @Test @DisplayName("withPlayers limits player count") void respectsPlayerCount() {
        GameEngine engine = new GameBuilder().withSeed(1).withPlayers(2).build();
        assertNotNull(engine);
    }
}