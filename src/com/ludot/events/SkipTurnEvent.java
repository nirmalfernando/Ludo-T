package com.ludot.events;

import com.ludot.model.Player;

// Event record published when a player's turn is skipped.

public record SkipTurnEvent(Player player) implements GameEvent {
}
