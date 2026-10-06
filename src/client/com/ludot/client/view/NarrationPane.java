package com.ludot.client.view;

import com.ludot.client.state.RoomModel;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * "What Just Happened": the last few narrated lines, always visible beside the board. A fast game scrolls the
 * event log and toasts away quickly, but this stays put, with the newest line in bold, so it is easy to follow.
 */
final class NarrationPane extends VBox {

    private static final int LINES = 4;

    private final RoomModel model;

    NarrationPane(RoomModel model) {
        super(3);
        this.model = model;
        getStyleClass().add("panel");
        setPadding(new javafx.geometry.Insets(12, 16, 12, 16));
        model.log().addListener((ListChangeListener<String>) change -> refresh());
        refresh();
    }

    private void refresh() {
        getChildren().clear();
        Label heading = new Label("What Just Happened");
        heading.setStyle("-fx-font-size: 11; -fx-text-fill: #78909c; -fx-font-weight: bold;");
        getChildren().add(heading);
        List<String> recent = recentEvents();
        for (int i = 0; i < recent.size(); i++) {
            boolean newest = i == recent.size() - 1;
            Label line = new Label(recent.get(i));
            line.setWrapText(true);
            line.setStyle(newest ? "-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #263238;"
                    : "-fx-font-size: 11; -fx-text-fill: #78909c;");
            getChildren().add(line);
        }
    }

    // the last few one-line events; the end-of-round status report is a long multi-line table, not something that happened
    private List<String> recentEvents() {
        List<String> events = new ArrayList<>();
        for (int i = model.log().size() - 1; i >= 0 && events.size() < LINES; i--) {
            String line = model.log().get(i);
            if (!line.contains("\n")) {
                events.add(0, line);
            }
        }
        return events;
    }
}
