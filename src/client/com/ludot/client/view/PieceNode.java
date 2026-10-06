package com.ludot.client.view;

import com.ludot.protocol.Colour;
import com.ludot.protocol.EffectType;
import com.ludot.protocol.Location;
import com.ludot.protocol.PieceDto;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

/** One piece on the board: a coloured disc with its number, an effect badge and, when movable, a pulsing ring. */
final class PieceNode extends Group {

    private static final double FLASH_MILLIS = 1600;

    private final Circle body;
    private final Circle ring;
    private final Circle halo;
    private final Label badge = new Label();
    private final ScaleTransition pulse;

    PieceNode(Colour colour, int index, double radius) {
        body = new Circle(radius, Theme.strong(colour));
        body.setStroke(Color.web("#263238"));
        body.setStrokeWidth(1.6);
        Text number = new Text(String.valueOf(index + 1));
        number.setFont(Font.font("System", FontWeight.BOLD, radius * 0.95));
        number.setFill(colour == Colour.YELLOW ? Color.web("#263238") : Color.WHITE);
        number.setTranslateX(-number.getLayoutBounds().getWidth() / 2);
        number.setTranslateY(number.getLayoutBounds().getHeight() / 4);
        ring = new Circle(radius + 4, Color.TRANSPARENT);
        ring.setStroke(Theme.HIGHLIGHT);
        ring.setStrokeWidth(3);
        ring.setVisible(false);
        halo = new Circle(radius + 7, Color.TRANSPARENT);
        halo.setStroke(Color.web("#ffb300"));
        halo.setStrokeWidth(4);
        halo.setOpacity(0);
        halo.setMouseTransparent(true);
        badge.setStyle("-fx-font-size: 11; -fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 6;"
                + "-fx-padding: 0 2;");
        badge.setTranslateX(radius * 0.2);
        badge.setTranslateY(-radius * 2.0);
        badge.setMouseTransparent(true);
        getChildren().addAll(halo, ring, body, number, badge);
        pulse = new ScaleTransition(Duration.millis(520), ring);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.25);
        pulse.setToY(1.25);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
    }

    /** Shows effect and briefing state as a small badge above the piece. */
    void showState(PieceDto dto) {
        String text = "";
        if (dto.location() == Location.BRIEFING) {
            text = "⛔" + dto.briefingRoundsLeft();
        } else if (dto.effect() == EffectType.ENERGISED) {
            text = "⚡" + dto.effectRoundsLeft();
        } else if (dto.effect() == EffectType.SICK) {
            text = "🤒" + dto.effectRoundsLeft();
        }
        badge.setText(text);
        badge.setVisible(!text.isEmpty());
    }

    /** Shows a gold halo that fades away, so the eye finds the piece that just moved. */
    void flash() {
        halo.setOpacity(1);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.millis(FLASH_MILLIS), halo);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.play();
    }

    void setMovable(boolean movable, Runnable onClick, Runnable onEnter, Runnable onExit) {
        ring.setVisible(movable);
        if (movable) {
            pulse.play();
            setCursor(Cursor.HAND);
            setOnMouseClicked(event -> onClick.run());
            setOnMouseEntered(event -> onEnter.run());
            setOnMouseExited(event -> onExit.run());
        } else {
            pulse.stop();
            ring.setScaleX(1);
            ring.setScaleY(1);
            setCursor(Cursor.DEFAULT);
            setOnMouseClicked(null);
            setOnMouseEntered(null);
            setOnMouseExited(null);
        }
    }
}
