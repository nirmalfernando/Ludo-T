package com.ludot.events;

// Every event must accept a visitor — enables type-safe dispatch without instanceof chains.
public interface GameEvent {
    void accept(EventVisitor visitor);
}
