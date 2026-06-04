package com.ludot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single cell on the Ludo board, tracking its type, colour affiliation, occupying pieces, and mystery status.
 * Implements no standalone design pattern, but acts as the Value Object shared across Board, RuleEngine, and MoveExecutor.
 */
public class Cell {

    private final int id;
    private final CellType cellType;
    private final Colour colour; // null for standard cells
    private final List<Piece> pieces;
    private boolean isMysteryCell;

    /** Constructs a cell with the given id, type, and optional colour affiliation. */
    public Cell(int id, CellType cellType, Colour colour) {
        this.id = id;
        this.cellType = cellType;
        this.colour = colour;
        this.pieces = new ArrayList<>();
    }

    /** Adds the given piece to this cell's occupant list. */
    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    /** Removes the given piece from this cell's occupant list. */
    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    /** Returns true if at least one piece is currently on this cell. */
    public boolean isOccupied() {
        return !pieces.isEmpty();
    }

    /** Returns true if this cell contains two or more pieces all of the same colour, forming a block. */
    public boolean hasBlock() {
        if (pieces.size() < 2) return false;
        Colour first = pieces.get(0).getColour();
        for (Piece p : pieces) {
            if (p.getColour() != first) return false;
        }
        return true;
    }

    /** Returns the number of pieces forming the block on this cell, or 0 if no block is present. */
    public int getBlockSize() {
        return hasBlock() ? pieces.size() : 0;
    }

    /** Returns the unique integer identifier of this cell. */
    public int getId() { return id; }

    /** Returns the type classification of this cell (e.g. STANDARD, START, APPROACH). */
    public CellType getCellType() { return cellType; }

    /** Returns the colour associated with this cell, or null for standard cells. */
    public Colour getColour() { return colour; }

    /** Returns true if this cell is currently designated as the active mystery cell. */
    public boolean isMysteryCell() { return isMysteryCell; }

    /** Marks or clears this cell as the active mystery cell. */
    public void setMysteryCell(boolean isMysteryCell) { this.isMysteryCell = isMysteryCell; }

    /** Returns an unmodifiable view of the pieces currently occupying this cell. */
    public List<Piece> getPieces() {
        return Collections.unmodifiableList(pieces);
    }
}
