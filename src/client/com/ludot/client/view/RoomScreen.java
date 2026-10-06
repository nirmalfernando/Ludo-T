package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.ClockSync;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.SeatDto;
import com.ludot.protocol.ServerMessage;
import java.util.UUID;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The game screen. It lays out the board, seats, dice, host controls, log and chat, wires them to the room's
 * model, and turns clicks into requests. It only shows what the server reports and decides nothing itself.
 */
public final class RoomScreen extends BorderPane {

    private final RoomModel model;
    private final Session session;
    private final Toasts toasts;
    private final Runnable onLeft;

    private final BoardView board = new BoardView();
    private final DicePane dice = new DicePane(this::roll);
    private final Label title = new Label();
    private final Label statusBar = new Label();
    private final Label pausedOverlay = new Label("Paused by the host");
    private final HostControls hostControls;
    private final GameOverPanel gameOver = new GameOverPanel(this::leaveRoom);
    private final RoomEventReactions reactions;
    private final TurnCountdown countdown;

    public RoomScreen(RoomModel model, Session session, Toasts toasts, Runnable onLeft, String myId,
                      ClockSync clock) {
        this.model = model;
        this.session = session;
        this.toasts = toasts;
        this.onLeft = onLeft;
        this.hostControls = new HostControls(model, this::send);
        this.reactions = new RoomEventReactions(dice, toasts);
        this.countdown = new TurnCountdown(model, clock, dice);
        setPadding(new Insets(10));
        setStyle(Theme.PANEL_STYLE);

        setTop(topBar());
        setLeft(leftColumn());
        setCenter(boardArea());
        setRight(new RoomTabs(model, myId, session));
        statusBar.setStyle("-fx-text-fill: #607d8b; -fx-font-size: 11;");
        statusBar.setPadding(new Insets(6, 0, 0, 0));
        setBottom(statusBar);

        wire();
        countdown.start(this);
        refreshAll();
        if (model.boardProperty().get() != null) {
            board.update(model.boardProperty().get(), false);
        }
    }

    // ------------------------------------------------------------------ layout

    private HBox topBar() {
        Button leave = new Button("Leave room");
        leave.setOnAction(event -> leaveRoom());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(12, title, spacer, hostControls, leave);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;
    }

    private VBox leftColumn() {
        SeatPanel seats = new SeatPanel(model, colour -> send(claim(colour)),
                () -> send(new ClientMessage.ReleaseSeat(UUID.randomUUID(), model.roomId())),
                () -> send(new ClientMessage.ReclaimSeat(UUID.randomUUID(), model.roomId())));
        VBox left = new VBox(12, seats, dice);
        left.setPrefWidth(250);
        left.setPadding(new Insets(0, 10, 0, 0));
        return left;
    }

    private ClientMessage.Request claim(Colour colour) {
        return new ClientMessage.ClaimSeat(UUID.randomUUID(), model.roomId(), colour);
    }

    private StackPane boardArea() {
        pausedOverlay.setStyle("-fx-background-color: rgba(38,50,56,0.82); -fx-text-fill: white; -fx-font-size: 26;"
                + "-fx-padding: 14 28; -fx-background-radius: 10;");
        pausedOverlay.setVisible(false);
        pausedOverlay.setMouseTransparent(true);
        StackPane area = new StackPane(board, pausedOverlay, gameOver);
        area.setAlignment(Pos.CENTER);
        return area;
    }

    // ------------------------------------------------------------------ reacting to the model

    private void wire() {
        model.boardProperty().addListener((obs, old, now) -> {
            if (now != null) {
                board.update(now, true);
            }
        });
        model.latestEventsProperty().addListener((obs, old, events) -> reactions.react(events));
        model.turnProperty().addListener((obs, old, now) -> refreshAll());
        model.statusProperty().addListener((obs, old, now) -> refreshAll());
        model.speedProperty().addListener((obs, old, now) -> refreshAll());
        model.hostIdProperty().addListener((obs, old, now) -> refreshAll());
        model.seats().addListener((ListChangeListener<SeatDto>) change -> refreshAll());
        model.myTurnProperty().addListener((obs, old, now) -> refreshMyTurn());
        model.standingsProperty().addListener((obs, old, now) -> gameOver.show(now));
        model.stateHashProperty().addListener((obs, old, now) -> refreshStatusBar());
    }

    private void refreshAll() {
        title.setText("Room " + model.roomId() + "  ·  " + model.variantProperty().get() + "  ·  "
                + (model.modeProperty().get() == RoomMode.BOT_MATCH ? "Bot match" : "Play") + "  ·  "
                + model.statusProperty().get());
        hostControls.refresh();
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
        if (offer == null || model.statusProperty().get() != RoomStatus.RUNNING) {
            dice.setBanner(TurnBanner.describe(model));
            return;
        }
        if (offer.phase() == Phase.AWAITING_ROLL) {
            dice.setBanner("Your turn: roll the dice");
            dice.setRollEnabled(true);
            return;
        }
        model.mySeat().map(SeatDto::colour).ifPresent(mine -> {
            dice.setBanner("You rolled " + offer.roll() + ": click a glowing piece");
            board.offerMoves(mine, offer.options(), option -> choose(offer, option.index()));
        });
    }

    private void refreshStatusBar() {
        var turn = model.turnProperty().get();
        statusBar.setText("seq " + model.seqProperty().get() + "   ·   state #"
                + String.format("%08X", model.stateHashProperty().get())
                + "   ·   turn " + (turn == null ? "-" : turn.turnNumber()) + "   ·   " + model.members().size() + " here");
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

    // a refused request shows its reason as a toast
    private void send(ClientMessage.Request request) {
        session.request(request, () -> { }, toasts::error);
    }

    private void leaveRoom() {
        session.request(new ClientMessage.LeaveRoom(UUID.randomUUID(), model.roomId()), response -> { });
        onLeft.run();
    }
}
