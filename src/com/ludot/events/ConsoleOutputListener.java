package com.ludot.events;

// Concrete Observer that prints formatted game events to the console.

public class ConsoleOutputListener implements GameEventListener {

    private final OutputFormatter formatter;

    public ConsoleOutputListener(OutputFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void onEvent(GameEvent event) {
    }
}
