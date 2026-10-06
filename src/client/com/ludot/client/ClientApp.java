package com.ludot.client;

import com.ludot.client.net.Session;
import com.ludot.client.state.ClientState;
import com.ludot.client.state.RoomModel;
import com.ludot.client.view.AppIcon;
import com.ludot.client.view.ConnectScreen;
import com.ludot.client.feedback.Cue;
import com.ludot.client.feedback.Feedback;
import com.ludot.client.view.ConnectionBadge;
import com.ludot.client.view.FeedbackToggles;
import com.ludot.client.view.LobbyScreen;
import com.ludot.client.view.RoomScreen;
import com.ludot.client.view.Theme;
import com.ludot.client.view.Toasts;
import com.ludot.protocol.ClientMessage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.ButtonBase;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The JavaFX application: owns the window, the connection and the client state, and decides which
 * screen is showing. Everything it shows comes from {@link ClientState}; nothing here knows the rules.
 */
public final class ClientApp extends Application {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;

    private final ClientState state = new ClientState(this::requestSnapshot);
    private final StackPane root = new StackPane();
    private final StackPane screenHolder = new StackPane();
    private final Toasts toasts = new Toasts(root);
    private Session session;
    private ConnectScreen.Request defaults = new ConnectScreen.Request(DEFAULT_HOST, DEFAULT_PORT, "");
    private String autoJoin;
    private com.ludot.protocol.Colour autoSeat;
    private String screenshotFile;
    private long screenshotAfterMs = 3_000;

    @Override
    public void start(Stage stage) {
        parseArguments(getParameters().getRaw());
        Theme.loadFonts();
        AppIcon.install(stage);
        clickSoundOnButtons();
        root.getChildren().add(screenHolder);
        stage.setTitle("LUDO-T Online");
        stage.setScene(new Scene(root, 1240, 780));
        stage.setMinWidth(1230);
        stage.setMinHeight(740);
        stage.show();

        state.roomProperty().addListener((obs, old, now) -> {
            if (now != null) {
                showRoom(now);
            } else if (session != null) {
                showLobby();
            }
        });
        state.connectionProperty().addListener((obs, old, now) -> {
            if (now == ClientState.Connection.DISCONNECTED && session != null) {
                session = null;
                state.leaveRoom();
                showConnect("The connection to the server was lost");
            }
        });
        showConnect(null);
        if (getParameters().getRaw().contains("--connect")) {
            connect(defaults, null);
        }
        if (screenshotFile != null) {
            PauseTransition wait = new PauseTransition(Duration.millis(screenshotAfterMs));
            wait.setOnFinished(event -> saveScreenshot(stage.getScene()));
            wait.play();
        }
    }

    // developer aid: --screenshot file.png [--screenshot-after ms] saves the window as a picture, then exits
    private void saveScreenshot(Scene scene) {
        try {
            WritableImage image = scene.snapshot(null);
            PixelReader pixels = image.getPixelReader();
            BufferedImage out = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < out.getHeight(); y++) {
                for (int x = 0; x < out.getWidth(); x++) {
                    out.setRGB(x, y, pixels.getArgb(x, y));
                }
            }
            javax.imageio.ImageIO.write(out, "png", new File(screenshotFile));
        } catch (IOException e) {
            System.err.println("Could not save screenshot: " + e.getMessage());
        }
        Platform.exit();
    }

    @Override
    public void stop() {
        if (session != null) {
            session.close();
        }
    }

    // ------------------------------------------------------------------ screens

    private void show(Node screen) {
        screenHolder.getChildren().setAll(screen);
    }

    private void showConnect(String note) {
        ConnectScreen screen = new ConnectScreen(defaults, note, request -> connect(request, screenReference()));
        show(screen);
    }

    private ConnectScreen screenReference() {
        return (ConnectScreen) screenHolder.getChildren().get(0);
    }

    private void showLobby() {
        show(new LobbyScreen(state, session, toasts, this::signOut));
    }

    // back to the first screen; the session is cleared first so closing it is not reported as a lost connection
    private void signOut() {
        Session old = session;
        session = null;
        state.leaveRoom();
        if (old != null) {
            old.close();
        }
        showConnect(null);
    }

    // every button press anywhere in the window makes a small click
    private void clickSoundOnButtons() {
        root.addEventFilter(ActionEvent.ACTION, event -> {
            if (event.getTarget() instanceof ButtonBase) {
                Feedback.get().play(Cue.CLICK);
            }
        });
    }

    // the sound and motion switches beside the connection light, shown in the room header
    private Node headerStatus() {
        HBox box = new HBox(14, new FeedbackToggles(), new ConnectionBadge(state));
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private void showRoom(RoomModel room) {
        show(new RoomScreen(room, session, toasts, this::leftRoom, state.myId(), state.clock(),
                headerStatus()));
    }

    private void leftRoom() {
        state.leaveRoom();
        if (session != null) {
            showLobby();
        }
    }

    // ------------------------------------------------------------------ connecting

    private void connect(ConnectScreen.Request request, ConnectScreen screen) {
        defaults = request;
        if (screen != null) {
            screen.setBusy(true);
        }
        Thread connector = new Thread(() -> {
            try {
                Session opened = Session.connect(request.host(), request.port(), request.nickname(), state);
                Platform.runLater(() -> {
                    session = opened;
                    showLobby();
                    if (autoJoin != null) {
                        opened.request(new ClientMessage.JoinRoom(UUID.randomUUID(), autoJoin), () -> {
                            if (autoSeat != null) {
                                opened.request(new ClientMessage.ClaimSeat(UUID.randomUUID(), autoJoin, autoSeat),
                                        () -> { }, toasts::error);
                            }
                        }, toasts::error);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ConnectScreen current = screenHolder.getChildren().isEmpty()
                            || !(screenHolder.getChildren().get(0) instanceof ConnectScreen) ? null : screenReference();
                    if (current != null) {
                        current.setBusy(false);
                        current.showError(friendly(e));
                    } else {
                        showConnect(friendly(e));
                    }
                });
            }
        }, "connector");
        connector.setDaemon(true);
        connector.start();
    }

    private static String friendly(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String text = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        return cause instanceof java.net.ConnectException ? "Could not reach the server (" + text + ")" : text;
    }

    private void requestSnapshot(String roomId) {
        if (session != null) {
            session.request(new ClientMessage.RequestSnapshot(UUID.randomUUID(), roomId), response -> { });
        }
    }

    private void parseArguments(List<String> args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        String nickname = "";
        for (int i = 0; i < args.size() - 1; i++) {
            switch (args.get(i)) {
                case "--host" -> host = args.get(i + 1);
                case "--port" -> port = Integer.parseInt(args.get(i + 1));
                case "--nickname" -> nickname = args.get(i + 1);
                case "--join" -> autoJoin = args.get(i + 1);
                case "--seat" -> autoSeat = com.ludot.protocol.Colour.valueOf(args.get(i + 1).toUpperCase());
                case "--screenshot" -> screenshotFile = args.get(i + 1);
                case "--screenshot-after" -> screenshotAfterMs = Long.parseLong(args.get(i + 1));
                default -> { }
            }
        }
        defaults = new ConnectScreen.Request(host, port, nickname);
    }
}
