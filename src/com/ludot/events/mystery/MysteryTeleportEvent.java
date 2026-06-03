package com.ludot.events.mystery;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;

public record MysteryTeleportEvent(Piece piece, Player player, Cell from, Cell to,
                                   String effectName, String effectDescription) implements GameEvent {
    public void accept(EventVisitor v) { v.visit(this); }
}
