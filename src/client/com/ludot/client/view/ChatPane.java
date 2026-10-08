package com.ludot.client.view;

import com.ludot.protocol.ServerMessage.ChatMessage;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import javafx.scene.shape.Circle;

public final class ChatPane extends BorderPane {

    private static final int MAX_LENGTH = 200;

    public interface Sender {
        void send(String text, Consumer<String> onRejected);
    }

    private final ListView<ChatMessage> list;
    private final TextField input = new TextField();
    private final Label hint = new Label();

    public ChatPane(ObservableList<ChatMessage> items, String myId, Sender sender) {
        list = new ListView<>(items);
        list.setFocusTraversable(false);
        list.setCellFactory(view -> new ChatCell(myId));
        input.setPromptText("Say Something…");
        input.getStyleClass().add("chat-input");
        input.setOnAction(event -> send(sender));
        Button send = new Button("Send");
        send.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        send.setOnAction(event -> send(sender));
        HBox.setHgrow(input, Priority.ALWAYS);
        HBox bar = new HBox(6, input, send);
        bar.setAlignment(Pos.CENTER);
        hint.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11;");
        javafx.scene.layout.VBox bottom = new javafx.scene.layout.VBox(4, hint, bar);
        bottom.setPadding(new Insets(6, 0, 0, 0));
        setCenter(list);
        setBottom(bottom);
        // scrolling at once can use stale estimates and land on older messages
        PauseTransition settle = new PauseTransition(Duration.millis(120));
        settle.setOnFinished(event -> list.scrollTo(items.size() - 1));
        items.addListener((ListChangeListener<ChatMessage>) change -> settle.playFromStart());
        list.heightProperty().addListener((obs, old, now) -> settle.playFromStart());
        settle.playFromStart();
    }

    private void send(Sender sender) {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > MAX_LENGTH) {
            hint.setText("Messages can be at most " + MAX_LENGTH + " characters");
            return;
        }
        hint.setText("");
        input.clear();
        sender.send(text, reason -> {
            hint.setText(reason);
            input.setText(text);
        });
    }

    private static final class ChatCell extends ListCell<ChatMessage> {
        private final String myId;

        ChatCell(String myId) {
            this.myId = myId;
        }

        @Override
        protected void updateItem(ChatMessage message, boolean empty) {
            super.updateItem(message, empty);
            if (empty || message == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label line = new Label(message.system() ? message.text()
                    : message.nickname() + (message.role() == com.ludot.protocol.Role.SPECTATOR ? " (Spectator)" : "")
                            + ": " + message.text());
            line.setWrapText(true);
            line.maxWidthProperty().bind(widthProperty().subtract(46));
            if (message.system()) {
                line.setStyle("-fx-font-style: italic; -fx-text-fill: #78909c;");
            } else if (message.senderId() != null && message.senderId().equals(myId)) {
                line.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565c0;");
            }
            HBox row = new HBox(6);
            row.setAlignment(Pos.TOP_LEFT);
            if (message.seatColour() != null) {
                row.getChildren().add(new Circle(5, Theme.strong(com.ludot.protocol.Colour.valueOf(message.seatColour().name()))));
            }
            row.getChildren().add(line);
            setText(null);
            setGraphic(row);
        }
    }
}
