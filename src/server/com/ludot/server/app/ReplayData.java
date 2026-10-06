package com.ludot.server.app;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Variant;
import java.util.List;

/**
 * Everything needed to play a finished game again: the rules and seed (the engine is deterministic) and every
 * decision in order. A roll step has no option index; a move step carries the chosen option.
 */
public record ReplayData(String gameId, Variant variant, long seed, Colour winner, List<Step> steps) {

    /** One applied engine step; {@code roll} is the number a roll step must produce again. */
    public record Step(int turnNumber, Colour colour, Integer roll, Integer optionIndex) {
    }

    public ReplayData {
        steps = List.copyOf(steps);
    }
}
