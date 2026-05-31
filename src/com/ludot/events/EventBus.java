package com.ludot.events;

import java.util.ArrayList;
import java.util.List;

// Event Bus implementing the Observer pattern; decouples event producers from consumers.

public class EventBus {

    private final List<GameEventListener> listeners = new ArrayList<>();

    public EventBus() {
    }

    public void subscribe(GameEventListener listener) {
        listeners.add(listener);
    }

    public void publish(GameEvent event) {
        for (GameEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
