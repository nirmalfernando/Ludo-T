package com.ludot.factory;

import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("PlayerFactory")
class PlayerFactoryTest {

    private Board board;
    @Test @DisplayName("createAll returns 4 players") void createAllFourPlayers() {
        assertEquals(4, PlayerFactory.createAll(board).size());
    }
    @Test @DisplayName("each player has 4 pieces") void eachPlayerFourPieces() {
        for (Player p : PlayerFactory.createAll(board))
            assertEquals(4, p.getPieces().size());
    }
    @Test @DisplayName("piece names follow initial+N pattern") void pieceNamingPattern() {
        Player red = PlayerFactory.createPlayer(Colour.RED, board);
        assertEquals("R1", red.getPieces().get(0).getName());
        assertEquals("R4", red.getPieces().get(3).getName());
    }
}