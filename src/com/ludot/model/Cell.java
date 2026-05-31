package com.ludot.model;

import java.util.List;

// Represents a single cell on the LUDO-T board, holding occupying pieces and cell metadata.

public class Cell {

    private int id;
    private CellType cellType;
    private Colour colour;
    private List<Piece> pieces;
    private boolean isMysteryCell;

    public Cell(int id, CellType cellType, Colour colour, List<Piece> pieces, boolean isMysteryCell) {
        this.id = id;
        this.cellType = cellType;
        this.colour = colour;
        this.pieces = pieces;
        this.isMysteryCell = isMysteryCell;
    }

    public void addPiece(Piece piece) {
    }

    public void removePiece(Piece piece) {
    }

    public boolean isOccupied() {
        return false;
    }

    public boolean hasBlock() {
        return false;
    }

    public int getBlockSize() {
        return 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CellType getCellType() {
        return cellType;
    }

    public void setCellType(CellType cellType) {
        this.cellType = cellType;
    }

    public Colour getColour() {
        return colour;
    }

    public void setColour(Colour colour) {
        this.colour = colour;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public void setPieces(List<Piece> pieces) {
        this.pieces = pieces;
    }

    public boolean isMysteryCell() {
        return isMysteryCell;
    }

    public void setMysteryCell(boolean isMysteryCell) {
        this.isMysteryCell = isMysteryCell;
    }
}
