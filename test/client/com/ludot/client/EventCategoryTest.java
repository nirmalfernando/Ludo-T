package com.ludot.client;

import com.ludot.client.view.EventCategory;
import com.ludot.protocol.EventType;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EventCategory (the event log filters)")
class EventCategoryTest {

    @Test @DisplayName("every event type belongs to a category, and every category has at least one type")
    void everyTypeIsPlaced() {
        Set<EventCategory> used = EnumSet.noneOf(EventCategory.class);
        for (EventType type : EventType.values()) {
            used.add(EventCategory.of(type));
        }
        assertEquals(EnumSet.allOf(EventCategory.class), used);
    }

    @Test @DisplayName("typical events land where a player would look for them")
    void sensibleGrouping() {
        assertEquals(EventCategory.DICE, EventCategory.of(EventType.DICE_ROLL));
        assertEquals(EventCategory.DICE, EventCategory.of(EventType.TRIPLE_SIX));
        assertEquals(EventCategory.MOVES, EventCategory.of(EventType.MOVE));
        assertEquals(EventCategory.MOVES, EventCategory.of(EventType.BLOCK_FORM));
        assertEquals(EventCategory.MOVES, EventCategory.of(EventType.HOME_REACH));
        assertEquals(EventCategory.CAPTURES, EventCategory.of(EventType.CAPTURE));
        assertEquals(EventCategory.SPECIALS, EventCategory.of(EventType.MYSTERY_TELEPORT));
        assertEquals(EventCategory.TURNS, EventCategory.of(EventType.TURN_START));
        assertEquals(EventCategory.TURNS, EventCategory.of(EventType.WIN));
    }

    @Test @DisplayName("with nothing selected every line passes, typed or not")
    void nothingSelectedShowsAll() {
        Set<EventCategory> none = EnumSet.noneOf(EventCategory.class);
        assertTrue(EventCategory.matches(none, EventType.MOVE));
        assertTrue(EventCategory.matches(none, null));
    }

    @Test @DisplayName("with categories selected only lines of those categories pass")
    void selectedCategoriesFilter() {
        Set<EventCategory> captures = EnumSet.of(EventCategory.CAPTURES);
        assertTrue(EventCategory.matches(captures, EventType.CAPTURE));
        assertFalse(EventCategory.matches(captures, EventType.MOVE));
        Set<EventCategory> two = EnumSet.of(EventCategory.CAPTURES, EventCategory.DICE);
        assertTrue(EventCategory.matches(two, EventType.DICE_ROLL));
        assertTrue(EventCategory.matches(two, EventType.CAPTURE));
        assertFalse(EventCategory.matches(two, EventType.TURN_START));
    }

    @Test @DisplayName("history with no type (from a snapshot) is hidden as soon as any category is chosen")
    void untypedHistoryOnlyUnderAll() {
        assertFalse(EventCategory.matches(EnumSet.of(EventCategory.MOVES), null));
    }
}
