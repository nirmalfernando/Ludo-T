package com.ludot.model;

import com.ludot.GameConstants;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the Ludo game board, providing access to standard path cells and colour-specific home straights.
 * Implements the Singleton pattern to ensure one shared board instance exists for the entire game.
 */
public class Board {

    private static Board instance;

    private List<Cell> cells;                        // 52-cell standard path (indices 0–51)
    private Map<Colour, List<Cell>> homeStraights;   // 5 home-straight cells per colour

    private Board() {}

    /** Returns the singleton Board instance, creating it lazily on first call. */
    public static Board getInstance() {
        if (instance == null) {
            instance = new Board();
        }
        return instance;
    }

    /** Destroys the singleton instance so tests can obtain a fresh board between test cases. */
    public static void resetInstance() {
        instance = null;
    }

    /** Returns the standard-path cell at the given index, throwing if the id is out of range. */
    public Cell getCell(int id) {
        if (id < 0 || id >= cells.size()) {
            throw new IllegalArgumentException("Cell id out of range: " + id);
        }
        return cells.get(id);
    }

    /** Returns an unmodifiable view of the home-straight cells for the given colour. */
    public List<Cell> getHomeStraight(Colour colour) {
        return Collections.unmodifiableList(homeStraights.get(colour));
    }

    /** Returns the approach cell for the given colour as defined by the board layout specification. */
    public Cell getApproachCell(Colour colour) {
        return switch (colour) {
            case YELLOW -> cells.get(GameConstants.YELLOW_APPROACH_CELL_ID);
            case BLUE   -> cells.get(GameConstants.BLUE_APPROACH_CELL_ID);
            case RED    -> cells.get(GameConstants.RED_APPROACH_CELL_ID);
            case GREEN  -> cells.get(GameConstants.GREEN_APPROACH_CELL_ID);
        };
    }

    /** Returns the start cell for the given colour as defined by the board layout specification. */
    public Cell getStartCell(Colour colour) {
        return switch (colour) {
            case YELLOW -> cells.get(GameConstants.YELLOW_START_CELL_ID);
            case BLUE   -> cells.get(GameConstants.BLUE_START_CELL_ID);
            case RED    -> cells.get(GameConstants.RED_START_CELL_ID);
            case GREEN  -> cells.get(GameConstants.GREEN_START_CELL_ID);
        };
    }

    /** Returns an unmodifiable view of the 52-cell standard circular path. */
    public List<Cell> getStandardPath() {
        return Collections.unmodifiableList(cells);
    }

    /** Initialises the board with the standard path and home-straight cells; called once by CellFactory at startup. */
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
