package com.ludot.client.view;

import com.ludot.protocol.Colour;
import com.ludot.protocol.RoomSummary;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

final class RoomCard extends HBox {

    private static final Colour[] SEAT_COLOURS = {Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE};
    private static final Color EMPTY_SEAT = Color.web("#d6dcf5");
    private static final double AI_OPACITY = 0.45;

    RoomCard(RoomSummary room, Consumer<RoomSummary> onOpen) {
        super(18);
        getStyleClass().add("room-card");
        setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(heading(room), seatStrip(room), details(room), spacer, button(room, onOpen));
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpen.accept(room);
            }
        });
    }

    private static VBox heading(RoomSummary room) {
        Label id = new Label(room.roomId());
        id.getStyleClass().add("room-id");
        HBox chips = new HBox(6,
                Chips.chip(RoomPresentation.statusLabel(room.status()), RoomPresentation.statusColour(room.status())),
                Chips.chip(RoomPresentation.modeLabel(room.mode()), Chips.MODE),
                Chips.chip(RoomPresentation.rulesLabel(room.variant()), Chips.RULES));
        VBox box = new VBox(6, id, chips);
        box.setMinWidth(210);
        return box;
    }

    private static VBox seatStrip(RoomSummary room) {
        HBox strip = new HBox(6);
        strip.setAlignment(Pos.BOTTOM_CENTER);
        List<RoomPresentation.SeatState> seats = RoomPresentation.seats(room);
        for (int i = 0; i < seats.size(); i++) {
            strip.getChildren().add(seat(seats.get(i), SEAT_COLOURS[i]));
        }
        Label people = new Label(RoomPresentation.peopleText(room));
        people.getStyleClass().add("muted");
        people.setStyle("-fx-font-size: 12;");
        VBox box = new VBox(4, strip, people);
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(150);
        return box;
    }

    private static PawnMascot seat(RoomPresentation.SeatState state, Colour colour) {
        PawnMascot pawn = switch (state) {
            case FREE -> new PawnMascot(EMPTY_SEAT, 40);
            default -> new PawnMascot(colour, 40);
        };
        if (state == RoomPresentation.SeatState.AI) {
            pawn.setOpacity(AI_OPACITY);
        }
        return pawn;
    }

    private static VBox details(RoomSummary room) {
        Label host = new Label("Host: " + room.hostNickname());
        Label watching = new Label(RoomPresentation.watchersText(room.spectators()));
        watching.getStyleClass().add("muted");
        watching.setStyle("-fx-font-size: 13;");
        return new VBox(4, host, watching);
    }

    private static Button button(RoomSummary room, Consumer<RoomSummary> onOpen) {
        Button button = new Button(RoomPresentation.actionLabel(RoomPresentation.action(room)));
        button.getStyleClass().add("blue-button");
        button.setOnAction(event -> onOpen.accept(room));
        return button;
    }
}
