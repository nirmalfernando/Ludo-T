package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.ClockSync;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.SeatDto;
import com.ludot.protocol.ServerMessage;
import java.util.UUID;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
public final class RoomScreen extends StackPane {

    private final RoomModel model;
    private final Session session;
    private final Toasts toasts;
    private final Runnable onLeft;
    private final Node connection;

    private final BoardView board = new BoardView();
    private final DicePane dice = new DicePane(this::roll);
    private final BorderPane layout = new BorderPane();
    private final RoomHeading heading;
    private final Label statusBar = new Label();
    private final Label pausedOverlay = new Label("Paused by the host");
    private final HostControls hostControls;
    private final RoomEnding ending = new RoomEnding(this::leaveRoom);
    private final RoomEventReactions reactions;
    private final TurnCountdown countdown;

    /** @param connection the connection indicator, shown beside the Leave button */
    public RoomScreen(RoomModel model, Session session, Toasts toasts, Runnable onLeft, String myId,
                      ClockSync clock, Node connection) {
        this.model = model;
        this.session = session;
        this.toasts = toasts;
        this.onLeft = onLeft;
        this.connection = connection;
        this.heading = new RoomHeading(model);
        this.hostControls = new HostControls(model, this::send);
        this.reactions = new RoomEventReactions(dice, toasts, board, model);
        this.countdown = new TurnCountdown(model, clock, dice);
        layout.setPadding(new Insets(14, 12, 10, 12));
        layout.setTop(topBar());
        layout.setLeft(leftColumn());
        layout.setCenter(boardArea());
        layout.setRight(new RoomTabs(model, myId, session));
        statusBar.getStyleClass().add("muted");
        statusBar.setStyle("-fx-font-size: 12;");
        statusBar.setPadding(new Insets(8, 0, 0, 0));
        layout.setBottom(statusBar);
        getChildren().addAll(new PlayfulBackground(33), layout);
        Theme.applyPlayful(this);

        wire();
        countdown.start(this);
        refreshAll();
        if (model.boardProperty().get() != null) {
            board.update(model.boardProperty().get(), false);
        }
    }

    // ------------------------------------------------------------------ layout

    private HBox topBar() {
        Button leave = new Button("Leave Room");
        leave.getStyleClass().add("ghost-button");
        leave.setOnAction(event -> leaveRoom());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(14, heading, spacer, hostControls, connection, leave);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;
    }

    private VBox leftColumn() {
        SeatPanel seats = new SeatPanel(model, colour -> send(claim(colour)),
                () -> send(new ClientMessage.ReleaseSeat(UUID.randomUUID(), model.roomId())),
                () -> send(new ClientMessage.ReclaimSeat(UUID.randomUUID(), model.roomId())));
        VBox left = new VBox(12, seats, dice, new NarrationPane(model.log()));
        left.setPrefWidth(245);
        left.setPadding(new Insets(0, 10, 0, 0));
        return left;
    }

    private ClientMessage.Request claim(Colour colour) {
        return new ClientMessage.ClaimSeat(UUID.randomUUID(), model.roomId(), colour);
    }

    private StackPane boardArea() {
        pausedOverlay.getStyleClass().add("paused-banner");
        pausedOverlay.setVisible(false);
        pausedOverlay.setMouseTransparent(true);
        StackPane area = new StackPane(board, pausedOverlay, ending);
        area.getStyleClass().add("board-frame");
        area.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
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
        model.standingsProperty().addListener((obs, old, now) -> ending.show(now));
        model.stateHashProperty().addListener((obs, old, now) -> refreshStatusBar());
    }

    private void refreshAll() {
        heading.refresh();
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
        statusBar.setText("Seq " + model.seqProperty().get() + "   ·   State #"
                + String.format("%08X", model.stateHashProperty().get())
                + "   ·   Turn " + (turn == null ? "-" : turn.turnNumber()) + "   ·   " + model.members().size() + " Here");
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
