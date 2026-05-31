package com.ludot.model;

import com.ludot.effects.Effect;

// Represents a single playing piece; delegates state-specific behaviour via the State pattern.

public class Piece {

    private String name;
    private Colour colour;
    private Cell position;
    private PieceState state;
    private Direction direction;
    private Direction originalDirection;
    private int captureCount;
    private Effect activeEffect;
    private int effectRoundsLeft;
    private int approachPassCount;

    public Piece(String name, Colour colour, Cell position, PieceState state,
                 Direction direction, Direction originalDirection, int captureCount,
                 Effect activeEffect, int effectRoundsLeft, int approachPassCount) {
        this.name = name;
        this.colour = colour;
        this.position = position;
        this.state = state;
        this.direction = direction;
        this.originalDirection = originalDirection;
        this.captureCount = captureCount;
        this.activeEffect = activeEffect;
        this.effectRoundsLeft = effectRoundsLeft;
        this.approachPassCount = approachPassCount;
    }

    public void move(int steps) {
    }

    public void sendToBase() {
    }

    public void resetInfo() {
    }

    public boolean canEnterHomeStraight() {
        return false;
    }

    public int getEffectiveSteps(int baseSteps) {
        return 0;
    }

    public void incrementCaptureCount() {
    }

    public String getName() {
        return name;
    }

    public Colour getColour() {
        return colour;
    }

    public Cell getPosition() {
        return position;
    }

    public void setPosition(Cell position) {
        this.position = position;
    }

    public PieceState getState() {
        return state;
    }

    public void setState(PieceState state) {
        this.state = state;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction getOriginalDirection() {
        return originalDirection;
    }

    public void setOriginalDirection(Direction originalDirection) {
        this.originalDirection = originalDirection;
    }

    public int getCaptureCount() {
        return captureCount;
    }

    public Effect getActiveEffect() {
        return activeEffect;
    }

    public void setActiveEffect(Effect activeEffect) {
        this.activeEffect = activeEffect;
    }

    public int getEffectRoundsLeft() {
        return effectRoundsLeft;
    }

    public int getApproachPassCount() {
        return approachPassCount;
    }

    public void incrementApproachPassCount() {
        this.approachPassCount++;
    }

    public void clearEffect() {
        this.activeEffect = new com.ludot.effects.NoEffect();
        this.effectRoundsLeft = 0;
    }

    public void tickEffect() {
        if (effectRoundsLeft > 0) {
            effectRoundsLeft--;
        }
        if (effectRoundsLeft == 0) {
            activeEffect = new com.ludot.effects.NoEffect();
        }
    }
}
