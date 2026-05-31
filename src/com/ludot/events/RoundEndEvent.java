package com.ludot.events;

// Event record published at the end of each game round.

public record RoundEndEvent(int roundNumber) implements GameEvent {
}
