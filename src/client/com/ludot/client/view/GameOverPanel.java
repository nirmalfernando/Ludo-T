package com.ludot.client.view;

import com.ludot.protocol.Colour;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The card shown over the board when a game ends: the winner's pawn and the final order. */
final class GameOverPanel extends VBox {

    private final Runnable onBack;

    GameOverPanel(Runnable onBack) {
        super(10);
        this.onBack = onBack;
        setAlignment(Pos.CENTER);
        getStyleClass().add("panel");
        setPadding(new Insets(24, 40, 24, 40));
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setVisible(false);
    }

    /** Shows the standings, or hides the panel when there are none (a new game). */
    void show(List<Colour> standings) {
        getChildren().clear();
        if (standings == null) {
            setVisible(false);
            return;
        }
        Colour winner = standings.get(0);
        Label heading = new Label(Theme.display(winner) + " Wins!");
        heading.setStyle("-fx-font-size: 34; -fx-text-fill: " + Theme.hex(winner) + ";");
        getChildren().addAll(new PawnMascot(winner, 90), heading);
        for (int place = 0; place < standings.size(); place++) {
            getChildren().add(row(place + 1, standings.get(place)));
        }
        Button back = new Button("Back to Lobby");
        back.getStyleClass().add("big-button");
        back.setOnAction(event -> onBack.run());
        VBox.setMargin(back, new Insets(10, 0, 0, 0));
        getChildren().add(back);
        setVisible(true);
    }

    private static HBox row(int place, Colour colour) {
        Label number = new Label(place + ".");
        number.setMinWidth(24);
        number.setStyle("-fx-font-size: 18;");
        Label name = new Label(Theme.display(colour));
        name.setStyle("-fx-font-size: 18;");
        HBox row = new HBox(10, number, new PawnMascot(colour, 30), name);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
