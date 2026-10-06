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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

        VBox text = new VBox(2, nameLabel(seat, mine), whoLabel(seat));
        if (isAway(seat, mine)) {
            text.getChildren().add(takeBackButton()); // under the labels: the card is too narrow for it beside them
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, new Circle(9, Theme.strong(seat.colour())), text, spacer, turnMark(active));
        row.setAlignment(Pos.CENTER_LEFT);
        seatAction(seat, mine, joinable).ifPresent(button -> row.getChildren().add(button));
        row.setStyle(Theme.CARD_STYLE + (active ? "-fx-border-color: #ffb300; -fx-border-width: 2;" : ""));
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
        name.setStyle("-fx-font-weight: bold;");
        return name;
    }

    private static Label whoLabel(SeatDto seat) {
        Label who = new Label(describe(seat));
        who.setStyle("-fx-text-fill: #546e7a; -fx-font-size: 12;");
        return who;
    }

    private static Label turnMark(boolean active) {
        Label mark = new Label(active ? "▶" : "");
        mark.setStyle("-fx-text-fill: #ff8f00; -fx-font-size: 16;");
        return mark;
    }

    private Button takeBackButton() {
        Button takeBack = new Button("Take back");
        takeBack.setStyle("-fx-base: #ffb300;");
        takeBack.setOnAction(event -> onReclaim.run());
        return takeBack;
    }

    // before the game: "Take" on a free seat, "Leave" on your own
    private Optional<Button> seatAction(SeatDto seat, boolean mine, boolean joinable) {
        if (joinable && seat.controller() == Controller.FREE) {
            Button take = new Button("Take");
            take.setOnAction(event -> onClaim.accept(seat.colour()));
            return Optional.of(take);
        }
        if (joinable && mine) {
            Button leave = new Button("Leave");
            leave.setOnAction(event -> onRelease.run());
            return Optional.of(leave);
        }
        return Optional.empty();
    }

    private static String describe(SeatDto seat) {
        return switch (seat.controller()) {
            case FREE -> "free";
            case AI -> "AI player";
            case HUMAN -> seat.nickname();
            case AFK -> seat.nickname() + " (away)";
            case DISCONNECTED -> seat.nickname() + " (offline)";
        };
    }
}
