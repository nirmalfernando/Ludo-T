package com.ludot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cell {

    private final int id;
    private final CellType cellType;
    private final Colour colour; // null for standard cells
    private final List<Piece> pieces;
    private boolean isMysteryCell;

    public Cell(int id, CellType cellType, Colour colour) {
        this.id = id;
        this.cellType = cellType;
        this.colour = colour;
        this.pieces = new ArrayList<>();
    }

    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    public boolean isOccupied() {
        return !pieces.isEmpty();
    }

    // a block is 2+ pieces of the same colour on the same cell
    public boolean hasBlock() {
        if (pieces.size() < 2) return false;
        Colour first = pieces.get(0).getColour();
        for (Piece p : pieces) {
            if (p.getColour() != first) return false;
        }
        return true;
    }

    public int getBlockSize() {
        return hasBlock() ? pieces.size() : 0;
    }

    public int getId() { return id; }
    public CellType getCellType() { return cellType; }
    public Colour getColour() { return colour; }
    public boolean isMysteryCell() { return isMysteryCell; }
    public void setMysteryCell(boolean isMysteryCell) { this.isMysteryCell = isMysteryCell; }

    // unmodifiable so callers can read but not directly mutate the list
    public List<Piece> getPieces() {
        return Collections.unmodifiableList(pieces);
    }
}
