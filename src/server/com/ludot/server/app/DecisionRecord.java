package com.ludot.server.app;

import com.ludot.protocol.Colour;

/**
 * One applied step. A roll step has optionIndex null and roll the number rolled; a move step carries the
 * chosen option and the roll it answers. turnNumber is the engine's step counter, unique within a room.
 */
public record DecisionRecord(String roomId, int turnNumber, Colour colour, Integer roll,
                             Integer optionIndex, DecisionSource source) implements WriteOp {
}
