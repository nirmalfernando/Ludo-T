package com.ludot.client.view;

import javafx.scene.control.Label;

final class Chips {

    static final String MODE = "#3d9bff";
    static final String RULES = "#8f7bd8";

    private Chips() {
    }

    static Label chip(String text, String colour) {
        Label chip = new Label(text);
        chip.getStyleClass().add("chip");
        chip.setStyle("-fx-background-color: " + colour + ";");
        return chip;
    }
}
