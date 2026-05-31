package com.ludot.model;

import java.util.List;
import com.ludot.strategy.PlayerStrategy;

// Represents a player in the game, owning a set of pieces and a decision strategy.

public class Player {

    private final Colour colour;
    private final List<Piece> pieces;
    private final PlayerStrategy strategy;

    public Player(Colour colour, List<Piece> pieces, PlayerStrategy strategy) {
        this.colour = colour;
        this.pieces = pieces;
        this.strategy = strategy;
    }

    public List<Piece> getPiecesOnBoard() {
        return List.of();
    }

    public List<Piece> getPiecesAtBase() {
        return List.of();
    }

    public List<Piece> getPiecesAtHome() {
        return List.of();
    }

    public boolean hasWon() {
        return false;
    }

    public Colour getColour() {
        return colour;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public PlayerStrategy getStrategy() {
        return strategy;
    }
}
