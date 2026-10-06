package com.ludot.client.view;

import com.ludot.protocol.Colour;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/** Everything that appears over the board when a game ends: the winner's card and the confetti. */
final class RoomEnding extends StackPane {

    private final GameOverPanel panel;
    private final ConfettiLayer confetti = new ConfettiLayer();

    RoomEnding(Runnable onBack) {
        this.panel = new GameOverPanel(onBack);
        setAlignment(Pos.CENTER);
        setPickOnBounds(false); // clicks outside the card fall through to the board
        getChildren().addAll(panel, confetti);
    }

    /** Shows the standings with a confetti burst, or clears everything when there are none (a new game). */
    void show(List<Colour> standings) {
        panel.show(standings);
        if (standings != null) {
            confetti.burst();
        }
    }
}
