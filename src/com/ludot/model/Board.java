package com.ludot.model;

import com.ludot.GameConstants;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    
    public static void resetInstance() {
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

    
    public Cell getApproachCell(Colour colour) {
        return switch (colour) {
            case YELLOW -> cells.get(GameConstants.YELLOW_APPROACH_CELL_ID);
            case BLUE   -> cells.get(GameConstants.BLUE_APPROACH_CELL_ID);
            case RED    -> cells.get(GameConstants.RED_APPROACH_CELL_ID);
            case GREEN  -> cells.get(GameConstants.GREEN_APPROACH_CELL_ID);
        };
    }

    
    public Cell getStartCell(Colour colour) {
        return switch (colour) {
            case YELLOW -> cells.get(GameConstants.YELLOW_START_CELL_ID);
            case BLUE   -> cells.get(GameConstants.BLUE_START_CELL_ID);
            case RED    -> cells.get(GameConstants.RED_START_CELL_ID);
            case GREEN  -> cells.get(GameConstants.GREEN_START_CELL_ID);
        };
    }

    
    public List<Cell> getStandardPath() {
        return Collections.unmodifiableList(cells);
    }

    
    public void initialize(List<Cell> cells, Map<Colour, List<Cell>> homeStraights) {
        this.cells = cells;
        this.homeStraights = homeStraights;
    }

    void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    void setHomeStraights(Map<Colour, List<Cell>> homeStraights) {
        this.homeStraights = homeStraights;
    }
}
