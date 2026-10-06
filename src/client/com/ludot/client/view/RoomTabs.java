package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.LogLine;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import java.util.UUID;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The right-hand side of the room screen: room chat and the narrated event log. */
final class RoomTabs extends TabPane {

    private static final double WIDTH = 310;

    RoomTabs(RoomModel model, String myId, Session session) {
        ChatPane chat = new ChatPane(model.chat(), myId, (text, onRejected) -> session.request(
                new ClientMessage.SendChat(UUID.randomUUID(), model.roomId(), text), response -> {
                    if (!response.isOk()) {
                        onRejected.accept(response.errorCode() == ErrorCode.CHAT_RATE_LIMITED
                                ? "You are sending too fast: slow down" : Session.describe(response));
                    }
                }));
        getTabs().addAll(fixed("Chat", chat), fixed("Event Log", eventLog(model)));
        setPrefWidth(WIDTH);
        setPadding(new Insets(0, 0, 0, 10));
    }

    // the narrated log with filter pills on top; filtering re-runs whenever the chosen categories change
    private static VBox eventLog(RoomModel model) {
        EventFilterBar bar = new EventFilterBar();
        FilteredList<LogLine> shown = new FilteredList<>(model.logLines());
        bar.filterProperty().addListener((obs, old, selected) ->
                shown.setPredicate(line -> EventCategory.matches(selected, line.type())));
        ListView<LogLine> log = new ListView<>(shown);
        log.setFocusTraversable(false);
        log.getStyleClass().add("event-log");
        Label empty = new Label("Nothing matches yet. History from before you joined only shows under All.");
        empty.setWrapText(true);
        empty.setStyle("-fx-font-size: 13; -fx-padding: 12;");
        log.setPlaceholder(empty);
        log.setCellFactory(view -> wrappingCell(log));
        shown.addListener((ListChangeListener<LogLine>) change ->
                Platform.runLater(() -> log.scrollTo(shown.size() - 1)));
        VBox box = new VBox(8, bar, log);
        VBox.setVgrow(log, Priority.ALWAYS);
        return box;
    }

    // long narration lines wrap instead of being cut off with an ellipsis
    private static ListCell<LogLine> wrappingCell(ListView<LogLine> log) {
        ListCell<LogLine> cell = new ListCell<>();
        cell.setWrapText(true);
        cell.prefWidthProperty().bind(log.widthProperty().subtract(26));
        cell.itemProperty().addListener((obs, old, line) -> cell.setText(line == null ? null : line.text()));
        return cell;
    }

    private static Tab fixed(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}
