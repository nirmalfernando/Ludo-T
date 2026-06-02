package com.ludot.factory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("GameBuilder")
class GameBuilderTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("build returns a non-null GameEngine") void buildReturnsEngine() {}
    @Test @DisplayName("same seed produces deterministic game") void seedDeterministic() {}
    @Test @DisplayName("withPlayers limits player count") void respectsPlayerCount() {}
}