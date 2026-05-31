package com.ludot.command;

// Command interface representing an executable game move (Command pattern).

public interface Move {

    MoveResult execute();

    String describe();
}
