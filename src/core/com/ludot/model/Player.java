package com.ludot.model;

import com.ludot.strategy.PlayerStrategy;
import java.util.ArrayList;
import com.ludot.model.state.PieceState;
import com.ludot.model.state.OnBoardState;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.state.AtHomeState;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.state.AtBaseState;
import java.util.List;

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
        List<Piece> result = new ArrayList<>();
        for (Piece p : pieces) {
            PieceState s = p.getState();
            if (s instanceof OnBoardState || s instanceof InHomeStraightState || s instanceof AtBriefingState) {
                result.add(p);
            }
        }
        return result;
    }

    
    public List<Piece> getPiecesAtBase() {
        List<Piece> result = new ArrayList<>();
        for (Piece p : pieces) {
            PieceState s = p.getState();
            if (s instanceof AtBaseState) {
                result.add(p);
            }
        }
        return result;
    }

    
    public List<Piece> getPiecesAtHome() {
        List<Piece> result = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.getState() instanceof AtHomeState) {
                result.add(p);
            }
        }
        return result;
    }

    
    public boolean hasWon() {
        return getPiecesAtHome().size() == pieces.size();
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
