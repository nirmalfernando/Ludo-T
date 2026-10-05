package com.ludot.events;

import java.util.ArrayList;
import java.util.List;

// Observer pattern hub — producers publish events here, listeners receive them.
public class EventBus {

    private final List<GameEventListener> listeners = new ArrayList<>();

    public void subscribe(GameEventListener listener) {
        listeners.add(listener);
    }

    public void publish(GameEvent event) {
        for (GameEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
