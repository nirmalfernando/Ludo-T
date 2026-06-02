package com.ludot.events.flow;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;

public record RoundEndEvent(int roundNumber) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}