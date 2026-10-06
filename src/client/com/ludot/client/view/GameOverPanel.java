package com.ludot.client.view;

import com.ludot.protocol.Colour;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** The overlay shown over the board when a game ends: who won and the final order. */
final class GameOverPanel extends VBox {

    private final Runnable onBack;

    GameOverPanel(Runnable onBack) {
        super(10);
        this.onBack = onBack;
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(255,255,255,0.96); -fx-padding: 24; -fx-background-radius: 12;"
                + "-fx-border-color: #90a4ae; -fx-border-radius: 12;");
        setMaxSize(320, 300);
        setVisible(false);
    }

    /** Shows the standings, or hides the panel when there are none (a new game). */
    void show(List<Colour> standings) {
        getChildren().clear();
        if (standings == null) {
            setVisible(false);
            return;
        }
        Label heading = new Label(Theme.display(standings.get(0)) + " wins!");
        heading.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: " + Theme.hex(standings.get(0)) + ";");
        getChildren().add(heading);
        for (int place = 0; place < standings.size(); place++) {
            Label row = new Label((place + 1) + ".  " + Theme.display(standings.get(place)));
            row.setStyle("-fx-font-size: 15;");
            getChildren().add(row);
        }
        Button back = new Button("Back to Lobby");
        back.setOnAction(event -> onBack.run());
        getChildren().add(back);
        setVisible(true);
    }
}
