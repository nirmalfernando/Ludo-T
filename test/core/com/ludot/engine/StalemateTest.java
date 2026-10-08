package com.ludot.engine;

import com.ludot.events.EventBus;
import com.ludot.events.flow.WinEvent;
import com.ludot.factory.GameBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Stalemate: jammed blockades cannot hang a game (T-3 forbids jumping them)")
class StalemateTest {

    @Test @DisplayName("seed 20 ends in a jam of four adjacent blockades; the game is adjudicated and announced once")
    void aJammedGameIsAdjudicated() {
        EventBus bus = new EventBus();
        List<Object> wins = new ArrayList<>();
        bus.subscribe(event -> { if (event instanceof WinEvent) wins.add(event); });
        GameEngine engine = new GameBuilder().withSeed(20).withEventBus(bus).build();

        engine.run();

        assertEquals(GamePhase.FINISHED, engine.getPhase());
        assertEquals(1, wins.size());
        assertNotNull(((WinEvent) wins.get(0)).winner());
    }
}
