package com.ludot.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// Singleton pattern — one shared board instance for the entire game.
public class Board {

    private static Board instance;

    private List<Cell> cells;                        // 52-cell standard path (indices 0–51)
    private Map<Colour, List<Cell>> homeStraights;   // 5 home-straight cells per colour

    private Board() {}

    public static Board getInstance() {
        if (instance == null) {
            instance = new Board();
        }
        return instance;
    }

    // package-private — only CellFactory calls this during board initialisation
    static void resetInstance() {
        instance = null;
    }

    public Cell getCell(int id) {
        if (id < 0 || id >= cells.size()) {
            throw new IllegalArgumentException("Cell id out of range: " + id);
        }
        return cells.get(id);
    }

    public List<Cell> getHomeStraight(Colour colour) {
        return Collections.unmodifiableList(homeStraights.get(colour));
    }

    // fixed approach cell positions from the board layout spec
    public Cell getApproachCell(Colour colour) {
        return switch (colour) {
            case YELLOW -> cells.get(50);
            case BLUE   -> cells.get(11);
            case RED    -> cells.get(24);
            case GREEN  -> cells.get(37);
        };
    }

    // fixed start cell positions from the board layout spec
    public Cell getStartCell(Colour colour) {
        return switch (colour) {
            case YELLOW -> cells.get(0);
            case BLUE   -> cells.get(13);
            case RED    -> cells.get(26);
            case GREEN  -> cells.get(39);
        };
    }

    public List<Cell> getStandardPath() {
        return Collections.unmodifiableList(cells);
    }

    void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    void setHomeStraights(Map<Colour, List<Cell>> homeStraights) {
        this.homeStraights = homeStraights;
    }
}
