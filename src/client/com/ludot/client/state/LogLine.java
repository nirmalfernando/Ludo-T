package com.ludot.client.state;

import com.ludot.protocol.EventType;

// one line of the event log. type says what kind of event it was so the log can be filtered
public record LogLine(EventType type, String text) {
}
