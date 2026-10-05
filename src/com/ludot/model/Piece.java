package com.ludot.model;

import com.ludot.GameConstants;
import com.ludot.effects.Effect;
import com.ludot.effects.NoEffect;
import com.ludot.model.state.PieceState;
import com.ludot.model.state.AtBaseState;

public class Piece {

    private final String name;
    private final Colour colour;
    private final Board board;      // the board this piece plays on (injected, one per game)
    private Cell position;          // null when at base
    private PieceState state;
    private Direction direction;    // assigned on leaving base via coin toss
    private Direction originalDirection; // stored when joining a block (T-5)
    private int captureCount;       // must be >= 1 to enter home straight in LudoT (T-7)
    private Effect activeEffect;
    private int approachPassCount;  // counts CCW passes of approach cell (T-1)

    
    public Piece(String name, Colour colour, Board board) {
        this.name = name;
        this.colour = colour;
        this.board = board;
        this.state = new AtBaseState();
        this.activeEffect = new NoEffect();
    }

    
    public void move(int steps) {
        int id = position.getId();

        // T-1: walk step-by-step so that passing THROUGH the approach cell mid-move is counted,
        // not just landing on it. A CCW piece skipping over cell 24 in a 6-step move still qualifies.
        for (int i = 0; i < steps; i++) {
            if (direction == Direction.CLOCKWISE) {
                id = (id + 1) % GameConstants.BOARD_SIZE;
            } else {
                id = ((id - 1) % GameConstants.BOARD_SIZE + GameConstants.BOARD_SIZE) % GameConstants.BOARD_SIZE;
            }
            if (direction == Direction.COUNTER_CLOCKWISE
                    && board.getCell(id).equals(board.getApproachCell(colour))) {
                incrementApproachPassCount();
            }
        }

        Cell newCell = board.getCell(id);
        position.removePiece(this);
        newCell.addPiece(this);
        position = newCell;
    }

    
    public Board getBoard() {
        return board;
    }

    
    public void sendToBase() {
        if (position != null) {
            position.removePiece(this);
            position = null;
        }
        state = new AtBaseState();
        direction = null;
    }

    
    public void resetInfo() {
        direction = null;
        originalDirection = null;
        captureCount = 0;
        approachPassCount = 0;
        clearEffect();
    }

    
    public boolean canEnterHomeStraight() {
        if (position == null) {
            return false;
        }
        return position.getCellType() == CellType.APPROACH
                && position.getColour() == colour;
    }

    
    public int getEffectiveSteps(int baseSteps) {
        return activeEffect.modifySteps(baseSteps);
    }

    
    public void incrementCaptureCount() {
        captureCount++;
    }

    
    public void incrementApproachPassCount() {
        approachPassCount++;
    }

    
    public void setActiveEffect(Effect effect) {
        this.activeEffect = effect;
    }

    
    public void clearEffect() {
        activeEffect = new NoEffect();
    }

    
    public void tickEffect() {
        activeEffect.tick();
        if (activeEffect.isExpired()) {
            activeEffect = new NoEffect();
        }
    }

    
    public String getName() { return name; }

    
    public Colour getColour() { return colour; }

    
    public Cell getPosition() { return position; }

    
    public void setPosition(Cell position) { this.position = position; }

    
    public PieceState getState() { return state; }

    
    public void setState(PieceState state) { this.state = state; }

    
    public Direction getDirection() { return direction; }

    
    public void setDirection(Direction direction) { this.direction = direction; }

    
    public Direction getOriginalDirection() { return originalDirection; }

    
    public void setOriginalDirection(Direction direction) { this.originalDirection = direction; }

    
    public int getCaptureCount() { return captureCount; }

    
    public int getApproachPassCount() { return approachPassCount; }

    
    public Effect getActiveEffect() { return activeEffect; }
}
