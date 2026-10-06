package com.ludot.client.view;

import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;

/** Small helpers for toggle buttons that act as radio buttons. */
final class Toggles {

    private Toggles() {
    }

    /** Clicking the selected button again would clear the group; this keeps one always selected. */
    static void keepSelected(ToggleButton button) {
        button.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (button.isSelected()) {
                event.consume();
            }
        });
    }
}
