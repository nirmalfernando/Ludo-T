package com.ludot.server.app;

import com.ludot.protocol.Colour;

/** One applied roll or move; optionIndex is null for a roll, roll is the number on the dice. */
public record DecisionRecord(String roomId, int turnNumber, Colour colour, Integer roll,
                             Integer optionIndex, DecisionSource source) {
}
