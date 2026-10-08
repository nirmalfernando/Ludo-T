package com.ludot.client.view;

import com.ludot.protocol.Colour;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

final class RoomEnding extends StackPane {

    private final GameOverPanel panel;
    private final ConfettiLayer confetti = new ConfettiLayer();

    RoomEnding(Runnable onBack) {
        this.panel = new GameOverPanel(onBack);
        setAlignment(Pos.CENTER);
        setPickOnBounds(false); // clicks outside the card fall through to the board
        getChildren().addAll(panel, confetti);
    }

    void show(List<Colour> standings) {
        panel.show(standings);
        if (standings != null) {
            confetti.burst();
        }
    }
}
