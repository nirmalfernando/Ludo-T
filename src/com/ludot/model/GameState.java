package com.ludot.model;

import com.ludot.GameConstants;
import java.util.ArrayList;
import java.util.List;

// Read-only snapshot of the game passed to player strategies so they can make informed decisions.
public class GameState {

    private final List<Player> players;
    private final Board board;
    private final int turnNumber; // monotonically increasing — strategies use this for stateless cycling

    // Convenience constructor for tests and legacy callers that don't need cycle count.
    public GameState(List<Player> players, Board board) {
        this(players, board, 0);
    }

    public GameState(List<Player> players, Board board, int turnNumber) {
        this.players = players;
        this.board = board;
        this.turnNumber = turnNumber;
    }

    // all pieces belonging to players other than the given colour
    public List<Piece> getOpponentPieces(Colour colour) {
        List<Piece> result = new ArrayList<>();
        for (Player p : players) {
            if (p.getColour() != colour) {
                result.addAll(p.getPieces());
            }
        }
        return result;
    }

    // true if the piece's destination (steps ahead) holds a single capturable opponent
    public boolean canCapture(Piece piece, int steps) {
        if (piece.getPosition() == null || piece.getDirection() == null) return false;
        int id = piece.getPosition().getId();
        int newId = piece.getDirection() == Direction.CLOCKWISE
                ? (id + steps) % GameConstants.BOARD_SIZE
                : ((id - steps) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
        Cell dest = board.getCell(newId);
        if (!dest.isOccupied() || dest.hasBlock()) return false;
        List<Piece> occ = dest.getPieces();
        return occ.size() == 1 && occ.get(0).getColour() != piece.getColour();
    }

    // all active blocks (2+ same-colour pieces on one cell) currently on the standard path
    public List<List<Piece>> findBlocks() {
        List<List<Piece>> blocks = new ArrayList<>();
        for (Cell cell : board.getStandardPath()) {
            if (cell.hasBlock()) {
                blocks.add(new ArrayList<>(cell.getPieces()));
            }
        }
        return blocks;
    }

    public List<Player> getPlayers() { return players; }
    public Board getBoard() { return board; }
    public int getTurnNumber() { return turnNumber; }
}
