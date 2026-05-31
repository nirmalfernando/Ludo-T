package com.ludot.model;

import java.util.List;
import java.util.Map;

// Singleton representing the LUDO-T board; ensures a single shared board instance.

public class Board {

    private static Board instance;

    private List<Cell> cells;
    private Map<Colour, List<Cell>> homeStraights;

    private Board() {
    }

    public static Board getInstance() {
        if (instance == null) {
            instance = new Board();
        }
        return instance;
    }

    static void resetInstance() {
        instance = null;
    }

    public Cell getCell(int id) {
        return null;
    }

    public List<Cell> getHomeStraight(Colour colour) {
        return List.of();
    }

    public Cell getApproachCell(Colour colour) {
        return null;
    }

    public Cell getStartCell(Colour colour) {
        return null;
    }

    public List<Cell> getStandardPath() {
        return List.of();
    }

    void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    void setHomeStraights(Map<Colour, List<Cell>> homeStraights) {
        this.homeStraights = homeStraights;
    }
}
