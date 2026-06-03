package com.ludot.events;
import com.ludot.events.flow.RoundEndEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("EventBus (Observer)")
class EventBusTest {
    @Test @DisplayName("subscribe adds listener") void subscribeAddsListener() {
        EventBus bus = new EventBus();
        List<GameEvent> received = new ArrayList<>();
        bus.subscribe(received::add);
        bus.publish(new RoundEndEvent(1, List.of()));
        assertEquals(1, received.size());
    }
    @Test @DisplayName("publish calls onEvent on all subscribers") void publishCallsAll() {
        EventBus bus = new EventBus();
        int[] count = {0};
        bus.subscribe(e -> count[0]++);
        bus.publish(new RoundEndEvent(1, List.of()));
        assertEquals(1, count[0]);
    }
    @Test @DisplayName("multiple listeners each receive the event") void multipleListeners() {
        EventBus bus = new EventBus();
        int[] count = {0};
        bus.subscribe(e -> count[0]++);
        bus.subscribe(e -> count[0]++);
        bus.publish(new RoundEndEvent(1, List.of()));
        assertEquals(2, count[0]);
    }
}