package com.ludot.events;

// Observer interface for components that react to published game events.

public interface GameEventListener {

    void onEvent(GameEvent event);
}
