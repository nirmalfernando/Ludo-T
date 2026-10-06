package com.ludot.client.view;

import com.ludot.protocol.EventType;
import java.util.Set;

/** The groups the event log can be filtered by. Every event type belongs to exactly one. */
public enum EventCategory {
    DICE("Dice"),
    MOVES("Moves"),
    CAPTURES("Captures"),
    SPECIALS("Specials"),
    TURNS("Turns");

    private final String label;

    EventCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** The category of an event type; the switch has no default, so a new event type will not compile until it is placed. */
    public static EventCategory of(EventType type) {
        return switch (type) {
            case ROLL_OFF, DICE_ROLL, COIN_TOSS, TRIPLE_SIX -> DICE;
            case MOVE, SKIP_TURN, PIECE_BLOCKED, APPROACH_PASS, BLOCK_FORM, BLOCK_MOVE, BLOCK_BREAK, HOME_ENTRY,
                 HOME_ENTRY_REJECTED, HOME_REACH, BRIEFING_ESCAPE -> MOVES;
            case CAPTURE -> CAPTURES;
            case MYSTERY_SPAWN, MYSTERY_TICK, MYSTERY_EXPIRE, MYSTERY_TELEPORT, EFFECT_APPLIED, EFFECT_EXPIRED -> SPECIALS;
            case GAME_START, TURN_ORDER, TURN_START, ROUND_END, WIN -> TURNS;
        };
    }

    /**
     * Whether a line passes the filter. With nothing selected everything passes (that is "All"), including old history
     * that has no type; with categories selected only typed lines of those categories pass.
     */
    public static boolean matches(Set<EventCategory> selected, EventType type) {
        if (selected.isEmpty()) {
            return true;
        }
        return type != null && selected.contains(of(type));
    }
}
