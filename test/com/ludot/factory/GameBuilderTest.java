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

    @Test @DisplayName("build returns a non-null GameEngine")
    void buildReturnsEngine() {
        assertNotNull(new GameBuilder().withSeed(42).withPlayers(4).build());
    }

    @Test @DisplayName("same seed produces the same first player (deterministic)")
    void seedDeterministic() {
        Board.resetInstance();
        GameEngine e1 = new GameBuilder().withSeed(99).withPlayers(4).build();
        Board.resetInstance();
        GameEngine e2 = new GameBuilder().withSeed(99).withPlayers(4).build();

        // same seed must produce the same roll-off winner
        var first1 = e1.determineFirstPlayer();
        Board.resetInstance();

        // rebuild e2 fresh so its dice sequence is at the start
        e2 = new GameBuilder().withSeed(99).withPlayers(4).build();
        var first2 = e2.determineFirstPlayer();

        assertEquals(first1.getColour(), first2.getColour(),
                "Same seed must always produce the same first player");
    }

    @Test @DisplayName("withPlayers(2) limits the game to exactly 2 players")
    void respectsPlayerCount() {
        GameEngine engine = new GameBuilder().withSeed(1).withPlayers(2).build();
        assertEquals(2, engine.getPlayers().size());
    }

    @Test @DisplayName("withPlayers(4) produces exactly 4 players")
    void respectsFourPlayerCount() {
        GameEngine engine = new GameBuilder().withSeed(1).withPlayers(4).build();
        assertEquals(4, engine.getPlayers().size());
    }
}
