package com.ludot.client.view;

import com.ludot.protocol.Colour;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** The first screen: who you are and, tucked away under "Server settings", where the server is. */
public final class ConnectScreen extends StackPane {

    /** What the user asked for. */
    public record Request(String host, int port, String nickname) {
    }

    /** Receives the user's request to connect. */
    public interface Handler {
        void connect(Request request);
    }

    private static final Colour[] TITLE_COLOURS = {Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE};

    private final TextField host = new TextField();
    private final TextField port = new TextField();
    private final TextField nickname = new TextField();
    private final Button connect = new Button("CONNECT");
    private final Label message = new Label();
    private final GridPane server = new GridPane();

    public ConnectScreen(Request defaults, String note, Handler handler) {
        host.setText(defaults.host());
        port.setText(String.valueOf(defaults.port()));
        nickname.setText(defaults.nickname());
        nickname.setPromptText("3-16 letters, digits or _");
        connect.getStyleClass().add("big-button");
        connect.setDefaultButton(true);
        connect.setOnAction(event -> submit(handler));
        showError(note);

        VBox content = new VBox(18, title(), pawns(), panel());
        content.setAlignment(Pos.CENTER);
        content.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        getChildren().addAll(new PlayfulBackground(7), content);
        setAlignment(Pos.CENTER);
        Theme.applyPlayful(this);
    }

    // LUDO in the four board colours, a die, then a T
    private static HBox title() {
        HBox letters = new HBox(2);
        letters.setAlignment(Pos.CENTER);
        String word = "LUDO";
        for (int i = 0; i < word.length(); i++) {
            letters.getChildren().add(letter(String.valueOf(word.charAt(i)), Theme.strong(TITLE_COLOURS[i])));
        }
        letters.getChildren().add(letter("-", Color.web("#2b3366")));
        letters.getChildren().add(letter("T", Color.web("#2b3366")));
        Label online = new Label("O N L I N E");
        online.getStyleClass().add("muted");
        online.setStyle("-fx-font-size: 18;");
        HBox row = new HBox(14, letters, online);
        row.setAlignment(Pos.BOTTOM_CENTER);
        return row;
    }

    private static Label letter(String text, Color colour) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 76; -fx-text-fill: " + web(colour) + ";"
                + "-fx-effect: dropshadow(gaussian, rgba(43,51,102,0.30), 6, 0.6, 0, 5);");
        return label;
    }

    private static HBox pawns() {
        HBox row = new HBox(22);
        row.setAlignment(Pos.CENTER);
        int step = 0;
        for (Colour colour : TITLE_COLOURS) {
            row.getChildren().add(new PawnMascot(colour, 78).bob(step++ * 220.0));
        }
        return row;
    }

    private VBox panel() {
        Label ask = new Label("What's your name?");
        ask.getStyleClass().add("panel-title");
        Button settings = new Button("Server settings  ▾");
        settings.getStyleClass().add("link-button");
        settings.setOnAction(event -> toggleServer(settings));
        buildServerForm();
        message.getStyleClass().add("error-banner");
        message.setWrapText(true);
        message.setMaxWidth(340);

        VBox panel = new VBox(12, ask, nickname, settings, server, connect, message);
        panel.getStyleClass().add("panel");
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(24, 32, 24, 32));
        panel.setMaxWidth(420);
        return panel;
    }

    private void buildServerForm() {
        server.setHgap(10);
        server.setVgap(8);
        server.addRow(0, new Label("Server"), host);
        server.addRow(1, new Label("Port"), port);
        host.setStyle("-fx-font-size: 15;");
        port.setStyle("-fx-font-size: 15;");
        setServerVisible(false);
    }

    private void toggleServer(Button settings) {
        boolean show = !server.isVisible();
        setServerVisible(show);
        settings.setText(show ? "Server settings  ▴" : "Server settings  ▾");
    }

    private void setServerVisible(boolean visible) {
        server.setVisible(visible);
        server.setManaged(visible);
    }

    private void submit(Handler handler) {
        int portNumber;
        try {
            portNumber = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException e) {
            setServerVisible(true);
            showError("The port must be a number");
            return;
        }
        showError(null);
        handler.connect(new Request(host.getText().trim(), portNumber, nickname.getText().trim()));
    }

    public void setBusy(boolean busy) {
        connect.setDisable(busy);
        connect.setText(busy ? "CONNECTING…" : "CONNECT");
    }

    public void showError(String text) {
        boolean has = text != null && !text.isBlank();
        message.setText(has ? text : "");
        message.setVisible(has);
        message.setManaged(has);
    }

    private static String web(Color c) {
        return String.format("#%02x%02x%02x", (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
