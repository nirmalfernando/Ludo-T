package com.ludot.factory;

import java.util.List;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.strategy.PlayerStrategy;

// Factory Method class that assembles fully configured Player instances for each colour.

public class PlayerFactory {

    private PlayerFactory() {
    }

    public static Player createPlayer(Colour colour) {
        return null;
    }

    public static List<Player> createAll() {
        return List.of();
    }

    private static PlayerStrategy createStrategy(Colour colour) {
        return null;
    }

    private static List<Piece> createPieces(Colour colour) {
        return List.of();
    }
}
