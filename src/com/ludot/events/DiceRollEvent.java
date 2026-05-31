package com.ludot.events;

import com.ludot.model.Player;

// Event record published when a player rolls the dice.

public record DiceRollEvent(Player player, int roll) implements GameEvent {
}
