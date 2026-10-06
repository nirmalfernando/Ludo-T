package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.Speed;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The game screen: board, seats, dice, host controls, event log and chat. It only shows what the server reports. */
public final class RoomScreen extends BorderPane {

    private static final Pattern ROLLED = Pattern.compile("rolled (\\d)");

    private final RoomModel model;
    private final Session session;
    private final Toasts toasts;
    private final Runnable onLeft;
    private final com.ludot.client.state.ClockSync clock;

    private final BoardView board = new BoardView();
    private final DicePane dice = new DicePane(this::roll);
    private final Label title = new Label();
    private final Label statusBar = new Label();
    private final Button start = new Button("Start game");
    private final Button pause = new Button("Pause");
    private final ChoiceBox<Speed> speed = new ChoiceBox<>();
    private final Label pausedOverlay = new Label("Paused by the host");
    private final VBox gameOver = new VBox(10);
    private boolean updatingSpeed;

    public RoomScreen(RoomModel model, Session session, Toasts toasts, Runnable onLeft, String myId,
                      com.ludot.client.state.ClockSync clock) {
        this.clock = clock;
        this.model = model;
        this.session = session;
        this.toasts = toasts;
        this.onLeft = onLeft;
        setPadding(new Insets(10));
        setStyle(Theme.PANEL_STYLE);

        setTop(buildTopBar());
        SeatPanel seats = new SeatPanel(model, this::claim, this::release, this::reclaim);
        VBox left = new VBox(12, seats, dice);
        left.setPrefWidth(250);
        left.setPadding(new Insets(0, 10, 0, 0));
        setLeft(left);
        setCenter(buildBoardArea());
        setRight(buildRightTabs(myId));
        statusBar.setStyle("-fx-text-fill: #607d8b; -fx-font-size: 11;");
        statusBar.setPadding(new Insets(6, 0, 0, 0));
        setBottom(statusBar);

        wire();
        startCountdown();
        refreshAll();
        if (model.boardProperty().get() != null) {
            board.update(model.boardProperty().get(), false);
        }
    }

    // ------------------------------------------------------------------ layout

    private HBox buildTopBar() {
        Button leave = new Button("Leave room");
        leave.setOnAction(event -> leaveRoom());
        speed.getItems().addAll(Speed.values());
        speed.setOnAction(event -> {
            if (!updatingSpeed && speed.getValue() != null && speed.getValue() != model.speedProperty().get()) {
                send(new ClientMessage.SetSpeed(UUID.randomUUID(), model.roomId(), speed.getValue()));
            }
        });
        start.setOnAction(event -> send(new ClientMessage.StartGame(UUID.randomUUID(), model.roomId())));
        pause.setOnAction(event -> send(model.statusProperty().get() == RoomStatus.PAUSED
                ? new ClientMessage.ResumeGame(UUID.randomUUID(), model.roomId())
                : new ClientMessage.PauseGame(UUID.randomUUID(), model.roomId())));
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label speedLabel = new Label("AI speed");
        HBox hostControls = new HBox(8, start, pause, speedLabel, speed);
        hostControls.setAlignment(Pos.CENTER);
        hostControls.managedProperty().bind(hostControls.visibleProperty());
        hostControls.visibleProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                model::isHost, model.hostIdProperty()));
        HBox bar = new HBox(12, title, spacer, hostControls, leave);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;
    }

    private StackPane buildBoardArea() {
        pausedOverlay.setStyle("-fx-background-color: rgba(38,50,56,0.82); -fx-text-fill: white; -fx-font-size: 26;"
                + "-fx-padding: 14 28; -fx-background-radius: 10;");
        pausedOverlay.setVisible(false);
        pausedOverlay.setMouseTransparent(true);
        gameOver.setAlignment(Pos.CENTER);
        gameOver.setStyle("-fx-background-color: rgba(255,255,255,0.96); -fx-padding: 24; -fx-background-radius: 12;"
                + "-fx-border-color: #90a4ae; -fx-border-radius: 12;");
        gameOver.setMaxSize(320, 300);
        gameOver.setVisible(false);
        StackPane area = new StackPane(board, pausedOverlay, gameOver);
        area.setAlignment(Pos.CENTER);
        return area;
    }

    private TabPane buildRightTabs(String myId) {
        ChatPane chat = new ChatPane(model.chat(), myId, (text, onRejected) -> session.request(
                new ClientMessage.SendChat(UUID.randomUUID(), model.roomId(), text), response -> {
                    if (!response.isOk()) {
                        onRejected.accept(response.errorCode() == ErrorCode.CHAT_RATE_LIMITED
                                ? "You are sending too fast: slow down" : Session.describe(response));
                    }
                }));
        ListView<String> log = new ListView<>(model.log());
        log.setFocusTraversable(false);
        model.log().addListener((ListChangeListener<String>) change ->
                Platform.runLater(() -> log.scrollTo(model.log().size() - 1)));
        Tab chatTab = new Tab("Chat", chat);
        Tab logTab = new Tab("Event log", log);
        chatTab.setClosable(false);
        logTab.setClosable(false);
        TabPane tabs = new TabPane(chatTab, logTab);
        tabs.setPrefWidth(340);
        tabs.setPadding(new Insets(0, 0, 0, 10));
        return tabs;
    }

    // ------------------------------------------------------------------ reacting to the model

    private void wire() {
        model.boardProperty().addListener((obs, old, now) -> {
            if (now != null) {
                board.update(now, true);
            }
        });
        model.latestEventsProperty().addListener((obs, old, events) -> onEvents(events));
        model.turnProperty().addListener((obs, old, now) -> refreshAll());
        model.statusProperty().addListener((obs, old, now) -> refreshAll());
        model.speedProperty().addListener((obs, old, now) -> refreshAll());
        model.hostIdProperty().addListener((obs, old, now) -> refreshAll());
        model.seats().addListener((ListChangeListener<com.ludot.protocol.SeatDto>) change -> refreshAll());
        model.myTurnProperty().addListener((obs, old, now) -> refreshMyTurn());
        model.standingsProperty().addListener((obs, old, now) -> showGameOver(now));
        model.stateHashProperty().addListener((obs, old, now) -> refreshStatusBar());
    }

    private void refreshAll() {
        title.setText("Room " + model.roomId() + "  ·  " + model.variantProperty().get() + "  ·  "
                + (model.modeProperty().get() == RoomMode.BOT_MATCH ? "Bot match" : "Play") + "  ·  "
                + model.statusProperty().get());
        boolean waiting = model.statusProperty().get() == RoomStatus.WAITING;
        boolean running = model.statusProperty().get() == RoomStatus.RUNNING || model.statusProperty().get() == RoomStatus.PAUSED;
        start.setVisible(waiting && model.modeProperty().get() == RoomMode.PLAY);
        start.setManaged(start.isVisible());
        pause.setVisible(running);
        pause.setManaged(running);
        pause.setText(model.statusProperty().get() == RoomStatus.PAUSED ? "Resume" : "Pause");
        updatingSpeed = true;
        speed.setValue(model.speedProperty().get());
        updatingSpeed = false;
        pausedOverlay.setVisible(model.statusProperty().get() == RoomStatus.PAUSED);
        var turn = model.turnProperty().get();
        board.setActiveColour(turn != null && model.statusProperty().get() == RoomStatus.RUNNING ? turn.colour() : null);
        refreshMyTurn();
        refreshStatusBar();
    }

    private void refreshMyTurn() {
        ServerMessage.YourTurn offer = model.myTurnProperty().get();
        board.clearMoves();
        dice.setRollEnabled(false);
        if (offer != null && model.statusProperty().get() == RoomStatus.RUNNING) {
            Colour mine = model.mySeat().map(com.ludot.protocol.SeatDto::colour).orElse(null);
            if (offer.phase() == Phase.AWAITING_ROLL) {
                dice.setBanner("Your turn: roll the dice");
                dice.setRollEnabled(true);
            } else if (mine != null) {
                dice.setBanner("You rolled " + offer.roll() + ": click a glowing piece");
                board.offerMoves(mine, offer.options(), option -> choose(offer, option.index()));
            }
            return;
        }
        dice.setBanner(describeTurn());
    }

    private String describeTurn() {
        var turn = model.turnProperty().get();
        RoomStatus status = model.statusProperty().get();
        if (status == RoomStatus.WAITING) {
            return model.modeProperty().get() == RoomMode.PLAY
                    ? "Take a seat, then the host starts the game" : "Starting…";
        }
        if (status == RoomStatus.FINISHED || turn == null) {
            return status == RoomStatus.FINISHED ? "Game over" : "";
        }
        String controller = model.seats().stream().filter(s -> s.colour() == turn.colour()).findFirst()
                .map(s -> s.controller() == com.ludot.protocol.Controller.HUMAN ? s.nickname() : "AI").orElse("");
        return "Waiting for " + Theme.display(turn.colour()) + " (" + controller + ")";
    }

    // everyone sees how long the player whose turn it is has left, in server time corrected for this machine's clock
    private void startCountdown() {
        javafx.animation.Timeline ticker = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(250), event -> updateCountdown()));
        ticker.setCycleCount(javafx.animation.Animation.INDEFINITE);
        ticker.play();
        sceneProperty().addListener((obs, old, now) -> {
            if (now == null) {
                ticker.stop(); // the screen was replaced
            }
        });
    }

    private void updateCountdown() {
        var turn = model.turnProperty().get();
        if (turn == null || turn.deadlineEpochMs() == null || model.statusProperty().get() != RoomStatus.RUNNING) {
            dice.setCountdown("", false);
            return;
        }
        long remaining = clock.remainingMs(turn.deadlineEpochMs(), System.currentTimeMillis());
        dice.setCountdown("⏱ " + Theme.display(turn.colour()) + ": " + com.ludot.client.state.ClockSync.format(remaining),
                remaining <= 5_000);
    }

    private void refreshStatusBar() {
        var turn = model.turnProperty().get();
        statusBar.setText("seq " + model.seqProperty().get() + "   ·   state #" + String.format("%08X", model.stateHashProperty().get())
                + "   ·   turn " + (turn == null ? "-" : turn.turnNumber()) + "   ·   " + model.members().size() + " here");
    }

    private void onEvents(List<EventDto> events) {
        for (EventDto event : events) {
            if (event.type() == EventType.DICE_ROLL) {
                Matcher matcher = ROLLED.matcher(event.text());
                if (matcher.find()) {
                    dice.showRoll(Integer.parseInt(matcher.group(1)));
                }
            } else if (event.type() == EventType.CAPTURE || event.type() == EventType.MYSTERY_TELEPORT
                    || event.type() == EventType.HOME_REACH || event.type() == EventType.TRIPLE_SIX
                    || event.type() == EventType.BRIEFING_ESCAPE) {
                toasts.info(event.text());
            }
        }
    }

    private void showGameOver(List<Colour> standings) {
        gameOver.getChildren().clear();
        if (standings == null) {
            gameOver.setVisible(false);
            return;
        }
        Label heading = new Label(Theme.display(standings.get(0)) + " wins!");
        heading.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: " + Theme.hex(standings.get(0)) + ";");
        gameOver.getChildren().add(heading);
        for (int place = 0; place < standings.size(); place++) {
            Label row = new Label((place + 1) + ".  " + Theme.display(standings.get(place)));
            row.setStyle("-fx-font-size: 15;");
            gameOver.getChildren().add(row);
        }
        Button back = new Button("Back to lobby");
        back.setOnAction(event -> leaveRoom());
        gameOver.getChildren().add(back);
        gameOver.setVisible(true);
    }

    // ------------------------------------------------------------------ actions

    private void roll() {
        ServerMessage.YourTurn offer = model.myTurnProperty().get();
        if (offer != null) {
            send(new ClientMessage.Roll(UUID.randomUUID(), model.roomId(), offer.turnNumber()));
        }
    }

    private void choose(ServerMessage.YourTurn offer, int optionIndex) {
        board.clearMoves();
        send(new ClientMessage.ChooseMove(UUID.randomUUID(), model.roomId(), offer.turnNumber(), optionIndex));
    }

    private void reclaim() {
        send(new ClientMessage.ReclaimSeat(UUID.randomUUID(), model.roomId()));
    }

    private void claim(Colour colour) {
        send(new ClientMessage.ClaimSeat(UUID.randomUUID(), model.roomId(), colour));
    }

    private void release() {
        send(new ClientMessage.ReleaseSeat(UUID.randomUUID(), model.roomId()));
    }

    private void send(ClientMessage.Request request) {
        session.request(request, () -> { }, toasts::error);
    }

    private void leaveRoom() {
        session.request(new ClientMessage.LeaveRoom(UUID.randomUUID(), model.roomId()), response -> { });
        onLeft.run();
    }
}
