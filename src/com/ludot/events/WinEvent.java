package com.ludot.events;

import com.ludot.model.Player;

// Event record published when a player wins the game.

public record WinEvent(Player winner, int totalRounds) implements GameEvent {
}
