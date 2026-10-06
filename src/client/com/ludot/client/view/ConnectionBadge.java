package com.ludot.client.view;

import com.ludot.client.state.ClientState;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * "● Connected · 12 ms": a softly pulsing green dot while the connection to the server is open, amber when the round
 * trip is slow, red when it is gone. It follows the live connection state, so it needs no refreshing.
 */
public final class ConnectionBadge extends HBox {

    private final ClientState state;
    private final Circle dot = new Circle(5);
    private final Label text = new Label();
    private final Tooltip tip = new Tooltip();

    public ConnectionBadge(ClientState state) {
        super(6);
        this.state = state;
        setAlignment(Pos.CENTER_LEFT);
        text.setStyle("-fx-font-size: 12; -fx-text-fill: #607d8b;");
        getChildren().addAll(dot, text);
        Tooltip.install(this, tip);
        state.connectionProperty().addListener((obs, old, now) -> refresh());
        state.latencyProperty().addListener((obs, old, now) -> refresh());
        pulse();
        refresh();
    }

    private void refresh() {
        var connection = state.connectionProperty().get();
        long latency = state.latencyProperty().get();
        text.setText(ConnectionPresentation.statusText(connection, latency));
        dot.setFill(Color.web(ConnectionPresentation.dotColour(connection, latency)));
        tip.setText(ConnectionPresentation.tooltip(connection, latency));
    }

    // a slow glow so the dot reads as "alive"; it stops by itself when the screen goes away
    private void pulse() {
        FadeTransition fade = new FadeTransition(Duration.millis(1100), dot);
        fade.setFromValue(1);
        fade.setToValue(0.4);
        fade.setAutoReverse(true);
        fade.setCycleCount(FadeTransition.INDEFINITE);
        Motion.whileShowing(this, fade);
    }
}
