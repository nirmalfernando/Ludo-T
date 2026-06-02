package com.ludot.factory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("PlayerFactory")
class PlayerFactoryTest {
    @Test @DisplayName("createAll returns 4 players") void createAllFourPlayers() {}
    @Test @DisplayName("each player has 4 pieces") void eachPlayerFourPieces() {}
    @Test @DisplayName("piece names follow ColourN pattern") void pieceNamingPattern() {}
}