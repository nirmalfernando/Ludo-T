package com.ludot.client.view;

import com.ludot.client.state.LogLine;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class EventLogPane extends VBox {

    EventLogPane(ObservableList<LogLine> lines) {
        super(8);
        EventFilterBar bar = new EventFilterBar();
        FilteredList<LogLine> shown = new FilteredList<>(lines);
        bar.filterProperty().addListener((obs, old, selected) ->
                shown.setPredicate(line -> EventCategory.matches(selected, line.type())));
        ListView<LogLine> log = new ListView<>(shown);
        log.setFocusTraversable(false);
        log.getStyleClass().add("event-log");
        log.setCellFactory(view -> wrappingCell(log));
        Label empty = new Label("Nothing matches yet. History from before you joined only shows under All.");
        empty.setWrapText(true);
        empty.setStyle("-fx-font-size: 13; -fx-padding: 12;");
        log.setPlaceholder(empty);
        shown.addListener((ListChangeListener<LogLine>) change ->
                Platform.runLater(() -> log.scrollTo(shown.size() - 1)));
        getChildren().addAll(bar, log);
        VBox.setVgrow(log, Priority.ALWAYS);
    }

    // long narration lines wrap instead of being cut off with an ellipsis
    private static ListCell<LogLine> wrappingCell(ListView<LogLine> log) {
        ListCell<LogLine> cell = new ListCell<>();
        cell.setWrapText(true);
        cell.prefWidthProperty().bind(log.widthProperty().subtract(26));
        cell.itemProperty().addListener((obs, old, line) -> show(cell, line));
        return cell;
    }

    // a multi-line entry (the end-of-round status table) shows its first line; hover for the whole table
    private static void show(ListCell<LogLine> cell, LogLine line) {
        if (line == null) {
            cell.setText(null);
            cell.setTooltip(null);
            return;
        }
        String text = line.text().strip();
        int newline = text.indexOf('\n');
        cell.setText(newline < 0 ? text : text.substring(0, newline).strip() + " ...");
        cell.setTooltip(newline < 0 ? null : new javafx.scene.control.Tooltip(line.text()));
    }
}
