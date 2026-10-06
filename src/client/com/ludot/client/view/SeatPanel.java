package com.ludot.client.view;

import com.ludot.client.state.RoomModel;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.SeatDto;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Four seat cards: who plays each colour, whose turn it is, and buttons to take or leave a seat before the game starts. */
public final class SeatPanel extends VBox {

    private final RoomModel model;
    private final Consumer<Colour> onClaim;
    private final Runnable onRelease;
    private final Runnable onReclaim;

    public SeatPanel(RoomModel model, Consumer<Colour> onClaim, Runnable onRelease, Runnable onReclaim) {
        super(8);
        this.model = model;
        this.onClaim = onClaim;
        this.onRelease = onRelease;
        this.onReclaim = onReclaim;
        model.seats().addListener((javafx.collections.ListChangeListener<SeatDto>) change -> rebuild());
        model.turnProperty().addListener((obs, old, now) -> rebuild());
        model.statusProperty().addListener((obs, old, now) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        boolean joinable = model.statusProperty().get() == RoomStatus.WAITING && model.modeProperty().get() == RoomMode.PLAY;
        for (SeatDto seat : model.seats()) {
            getChildren().add(card(seat, joinable));
        }
    }

    private HBox card(SeatDto seat, boolean joinable) {
        boolean mine = model.mySeat().map(s -> s.colour() == seat.colour()).orElse(false);
        boolean active = isActive(seat);

        VBox text = new VBox(2, nameLabel(seat, mine), whoRow(seat));
        if (isAway(seat, mine)) {
            text.getChildren().add(takeBackButton()); // under the labels: the card is too narrow for it beside them
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, new PawnMascot(seat.colour(), 40), text, spacer, turnMark(active));
        row.setAlignment(Pos.CENTER_LEFT);
        seatAction(seat, mine, joinable).ifPresent(button -> row.getChildren().add(button));
        row.getStyleClass().add("seat-card");
        if (active) {
            row.getStyleClass().add("seat-active");
        }
        return row;
    }

    private boolean isActive(SeatDto seat) {
        var turn = model.turnProperty().get();
        return model.statusProperty().get() == RoomStatus.RUNNING && turn != null && turn.colour() == seat.colour();
    }

    private boolean isAway(SeatDto seat, boolean mine) {
        boolean waitingForYou = seat.controller() == Controller.AFK || seat.controller() == Controller.DISCONNECTED;
        return mine && waitingForYou && model.statusProperty().get() == RoomStatus.RUNNING;
    }

    private static Label nameLabel(SeatDto seat, boolean mine) {
        Label name = new Label(Theme.display(seat.colour()) + (mine ? "  (you)" : ""));
        name.getStyleClass().add("seat-name");
        return name;
    }

    // the player's name, led by a dot showing whether they are connected (green), away (amber) or offline (red)
    private static HBox whoRow(SeatDto seat) {
        Label who = new Label(describe(seat));
        who.setStyle("-fx-text-fill: #546e7a; -fx-font-size: 12;");
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        ConnectionPresentation.seatDot(seat.controller()).ifPresent(colour -> {
            Circle dot = new Circle(4.5, Color.web(colour));
            Tooltip.install(dot, new Tooltip(ConnectionPresentation.seatDotTip(seat.controller())));
            row.getChildren().add(dot);
        });
        row.getChildren().add(who);
        return row;
    }

    private static Label turnMark(boolean active) {
        Label mark = new Label(active ? "▶" : "");
        mark.setStyle("-fx-text-fill: #ff8f00; -fx-font-size: 16;");
        return mark;
    }

    private Button takeBackButton() {
        Button takeBack = new Button("Take back");
        takeBack.getStyleClass().addAll("amber-button", "small-button");
        takeBack.setOnAction(event -> onReclaim.run());
        return takeBack;
    }

    // before the game: "Take" on a free seat, "Leave" on your own
    private Optional<Button> seatAction(SeatDto seat, boolean mine, boolean joinable) {
        if (joinable && seat.controller() == Controller.FREE) {
            Button take = new Button("Take");
            take.getStyleClass().addAll("blue-button", "small-button");
            take.setOnAction(event -> onClaim.accept(seat.colour()));
            return Optional.of(take);
        }
        if (joinable && mine) {
            Button leave = new Button("Leave");
            leave.getStyleClass().addAll("ghost-button", "small-button");
            leave.setOnAction(event -> onRelease.run());
            return Optional.of(leave);
        }
        return Optional.empty();
    }

    private static String describe(SeatDto seat) {
        return switch (seat.controller()) {
            case FREE -> "Free";
            case AI -> "AI player";
            case HUMAN -> seat.nickname();
            case AFK -> seat.nickname() + " (away)";
            case DISCONNECTED -> seat.nickname() + " (offline)";
        };
    }
}
