package com.ludot.client.view;

import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;

final class Toggles {

    private Toggles() {
    }

    static void keepSelected(ToggleButton button) {
        button.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (button.isSelected()) {
                event.consume();
            }
        });
    }
}
