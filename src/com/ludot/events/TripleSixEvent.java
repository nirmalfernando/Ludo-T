package com.ludot.events;

import com.ludot.model.Player;

// Event record published when a player rolls three consecutive sixes (triple-six penalty rule).

public record TripleSixEvent(Player player) implements GameEvent {
}
