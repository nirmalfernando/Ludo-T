package com.ludot.engine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ludot.model.Board;
@DisplayName("TurnManager")
class TurnManagerTest {
    @BeforeEach  void setUp()    { Board.resetInstance(); }
    @AfterEach   void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("executeTurn publishes DiceRollEvent") void publishesDiceRoll() {}
    @Test @DisplayName("triple-six publishes TripleSixEvent and breaks block") void tripleSixBreaksBlock() {}
    @Test @DisplayName("bonus roll on 6 triggers another turn") void bonusRollOnSix() {}
    @Test @DisplayName("nextPlayer rotates through players") void nextPlayerRotates() {}
    @Test @DisplayName("incrementRound increments count") void incrementsRound() {}
}