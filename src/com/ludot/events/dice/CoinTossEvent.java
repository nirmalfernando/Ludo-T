package com.ludot.events.dice;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Direction;
import com.ludot.model.Piece;

public record CoinTossEvent(Piece piece, Direction direction) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}