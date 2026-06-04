package com.ludot.factory;

import com.ludot.model.Colour;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.strategy.BlueStrategy;
import com.ludot.strategy.GreenStrategy;
import com.ludot.strategy.PlayerStrategy;
import com.ludot.strategy.RedStrategy;
import com.ludot.strategy.YellowStrategy;
import java.util.ArrayList;
import java.util.List;

// Factory Method — assembles fully configured Player instances for each colour.
public final class PlayerFactory {

    private static final int PIECES_PER_PLAYER = 4;

    private PlayerFactory() {
    }

    public static Player createPlayer(Colour colour) {
        return new Player(colour, createPieces(colour), createStrategy(colour));
    }

    public static List<Player> createAll() {
        List<Player> players = new ArrayList<>();
        for (Colour c : Colour.values()) {
            players.add(createPlayer(c));
        }
        return players;
    }

    private static PlayerStrategy createStrategy(Colour colour) {
        return switch (colour) {
            case RED    -> new RedStrategy();
            case GREEN  -> new GreenStrategy();
            case YELLOW -> new YellowStrategy();
            case BLUE   -> new BlueStrategy();
        };
    }

    private static List<Piece> createPieces(Colour colour) {
        List<Piece> pieces = new ArrayList<>(PIECES_PER_PLAYER);
        String prefix = String.valueOf(colour.name().charAt(0));
        for (int i = 1; i <= PIECES_PER_PLAYER; i++) {
            pieces.add(new Piece(prefix + i, colour));
        }
        return pieces;
    }
}
