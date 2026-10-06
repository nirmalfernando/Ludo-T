package com.ludot.client.view;

import com.ludot.protocol.FinishedGame;
import java.time.ZoneId;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** One finished game in the list: its id and rules, who won, how long it was, when, and a REPLAY button. */
final class FinishedGameCard extends HBox {

    FinishedGameCard(FinishedGame game, Consumer<FinishedGame> onReplay) {
        super(18);
        getStyleClass().add("room-card");
        setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getChildren().addAll(heading(game), winner(game), spacer, replayButton(game, onReplay));
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onReplay.accept(game);
            }
        });
    }

    private static VBox heading(FinishedGame game) {
        Label id = new Label(game.gameId());
        id.getStyleClass().add("room-id");
        HBox chips = new HBox(6, Chips.chip(RoomPresentation.rulesLabel(game.variant()), Chips.RULES),
                Chips.chip(RoomPresentation.modeLabel(game.mode()), Chips.MODE));
        VBox box = new VBox(6, id, chips);
        box.setMinWidth(200);
        return box;
    }

    private static HBox winner(FinishedGame game) {
        Label who = new Label(FinishedGamePresentation.winnerText(game.winner()));
        who.setStyle("-fx-font-size: 18; -fx-text-fill: " + Theme.hex(game.winner()) + ";");
        Label detail = new Label(FinishedGamePresentation.stepsText(game.turns()) + "  ·  "
                + FinishedGamePresentation.dateText(game.finishedAt(), ZoneId.systemDefault()));
        detail.getStyleClass().add("muted");
        detail.setStyle("-fx-font-size: 13;");
        HBox box = new HBox(12, new PawnMascot(game.winner(), 46), new VBox(2, who, detail));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button replayButton(FinishedGame game, Consumer<FinishedGame> onReplay) {
        Button button = new Button("REPLAY");
        button.getStyleClass().add("blue-button");
        button.setOnAction(event -> onReplay.accept(game));
        return button;
    }
}
