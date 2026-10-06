package com.ludot.client.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The first screen: where the server is and who you are. */
public final class ConnectScreen extends StackPane {

    /** What the user asked for. */
    public record Request(String host, int port, String nickname) {
    }

    /** Receives the user's request to connect. */
    public interface Handler {
        void connect(Request request);
    }

    private final TextField host = new TextField();
    private final TextField port = new TextField();
    private final TextField nickname = new TextField();
    private final Button connect = new Button("Connect");
    private final Label message = new Label();

    public ConnectScreen(Request defaults, String note, Handler handler) {
        setStyle(Theme.PANEL_STYLE);
        Label title = new Label("LUDO-T Online");
        title.setStyle("-fx-font-size: 30; -fx-font-weight: bold;");
        Label subtitle = new Label("Play Ludo with a twist, against people and AI");
        subtitle.setStyle("-fx-text-fill: #607d8b;");

        host.setText(defaults.host());
        port.setText(String.valueOf(defaults.port()));
        nickname.setText(defaults.nickname());
        nickname.setPromptText("3-16 letters, digits or _");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Server"), host);
        form.addRow(1, new Label("Port"), port);
        form.addRow(2, new Label("Nickname"), nickname);
        connect.setDefaultButton(true);
        connect.setOnAction(event -> submit(handler));
        message.setWrapText(true);
        message.setMaxWidth(320);
        message.setText(note == null ? "" : note);
        message.setStyle("-fx-text-fill: #c62828;");

        VBox card = new VBox(14, title, subtitle, form, connect, message);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.setPadding(new Insets(28));
        card.setStyle(Theme.CARD_STYLE);
        getChildren().add(card);
        setAlignment(Pos.CENTER);
    }

    private void submit(Handler handler) {
        int portNumber;
        try {
            portNumber = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException e) {
            showError("The port must be a number");
            return;
        }
        message.setText("");
        handler.connect(new Request(host.getText().trim(), portNumber, nickname.getText().trim()));
    }

    public void setBusy(boolean busy) {
        connect.setDisable(busy);
        connect.setText(busy ? "Connecting…" : "Connect");
    }

    public void showError(String text) {
        message.setText(text);
    }
}
