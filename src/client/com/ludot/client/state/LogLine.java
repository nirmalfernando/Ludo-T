package com.ludot.client.state;

import com.ludot.protocol.EventType;

/**
 * One line of the event log. {@code type} says what kind of event it was so the log can be filtered; it is null for
 * history that arrived inside a snapshot (before this client joined), which only has text.
 */
public record LogLine(EventType type, String text) {
}
