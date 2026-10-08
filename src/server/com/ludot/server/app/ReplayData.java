package com.ludot.server.app;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Variant;
import java.util.List;

public record ReplayData(String gameId, Variant variant, long seed, Colour winner, List<Step> steps) {

    public record Step(int turnNumber, Colour colour, Integer roll, Integer optionIndex) {
    }

    public ReplayData {
        steps = List.copyOf(steps);
    }
}
