package com.ludot.events.flow;

import com.ludot.events.EventVisitor;
import com.ludot.events.GameEvent;
import com.ludot.model.Cell;
import com.ludot.model.Player;
import java.util.List;

public record RoundEndEvent(int roundNumber, List<Player> players, Cell mysteryCell, int mysteryRoundsLeft) implements GameEvent {

    public static RoundEndEvent of(int roundNumber, List<Player> players) {
        return new RoundEndEvent(roundNumber, players, null, 0);
    }

    public void accept(EventVisitor v) {
        v.visit(this);
    }
}
