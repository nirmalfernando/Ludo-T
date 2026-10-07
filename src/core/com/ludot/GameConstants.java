package com.ludot;

// Single source of truth for all game-wide numeric constants.
// Every magic number in the codebase should trace back to a declaration here.
public final class GameConstants {

    private GameConstants() {
    }

    // Board layout
    public static final int BOARD_SIZE           = 52; // standard circular path cell count
    public static final int HOME_STRAIGHT_LENGTH = 5;  // cells per colour including the HOME cell

    // Start cell IDs — shared between Board and CellFactory to avoid duplication
    public static final int YELLOW_START_CELL_ID =  0;
    public static final int BLUE_START_CELL_ID   = 13;
    public static final int RED_START_CELL_ID    = 26;
    public static final int GREEN_START_CELL_ID  = 39;

    // Approach cell IDs — the cell just before each colour's home straight entry
    public static final int YELLOW_APPROACH_CELL_ID = 50;
    public static final int BLUE_APPROACH_CELL_ID   = 11;
    public static final int RED_APPROACH_CELL_ID    = 24;
    public static final int GREEN_APPROACH_CELL_ID  = 37;

    // Dice mechanics
    // T-11: Alpha, Beta and Gamma are the 9th, 27th and 46th cells counted from yellow's approach cell (counted as 0)
    public static final int ALPHA_CELL_ID = (YELLOW_APPROACH_CELL_ID + 9) % BOARD_SIZE;
    public static final int BETA_CELL_ID  = (YELLOW_APPROACH_CELL_ID + 27) % BOARD_SIZE;
    public static final int GAMMA_CELL_ID = (YELLOW_APPROACH_CELL_ID + 46) % BOARD_SIZE;

    public static final int LEAVE_BASE_ROLL          = 6; // roll needed to leave base / trigger bonus
    public static final int CONSECUTIVE_SIXES_LIMIT  = 3; // three consecutive 6s = triple-six penalty

    // Mystery cell
    public static final int STALEMATE_ROUNDS = 100; // rounds in which no piece moves before a game is adjudicated
    public static final int MYSTERY_LIFESPAN = 4; // rounds a mystery cell remains active (T-10)

    // Briefing (Beta) state
    public static final int BRIEFING_DURATION    = 4; // rounds a piece is locked after Beta (T-13)
    public static final int BRIEFING_ESCAPE_ROLL  = 3; // dice value that counts toward triple-three escape
    public static final int BRIEFING_ESCAPE_COUNT = 3; // consecutive rolls needed to escape early

    // Effects (Alpha)
    public static final int EFFECT_DURATION = 4; // rounds an Energised/Sick effect lasts (T-12)

    // Engine safety
    public static final int MAX_BONUS_DEPTH = 5; // maximum recursive bonus-roll depth guard
}
