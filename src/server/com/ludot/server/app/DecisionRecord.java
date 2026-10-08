package com.ludot.server.app;

import com.ludot.protocol.Colour;

public record DecisionRecord(String roomId, int turnNumber, Colour colour, Integer roll,
                             Integer optionIndex, DecisionSource source) implements WriteOp {
}
