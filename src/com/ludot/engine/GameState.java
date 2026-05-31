package com.ludot.engine;

import java.util.List;
import com.ludot.model.Board;
import com.ludot.model.Colour;
import com.ludot.model.Piece;
import com.ludot.model.Player;

// Read-only snapshot of the game passed to player strategies (immutable view pattern).

public class GameState {

    private final List<Player> players;
    private final Board board;

    public GameState(List<Player> players, Board board) {
        this.players = players;
        this.board = board;
    }

    public List<Piece> getOpponentPieces(Colour colour) {
        return List.of();
    }

    public boolean canCapture(Piece piece, int steps) {
        return false;
    }

    public List<List<Piece>> findBlocks() {
        return List.of();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Board getBoard() {
        return board;
    }
}
