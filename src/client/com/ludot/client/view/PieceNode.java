package com.ludot.client.view;

import com.ludot.client.feedback.Feedback;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EffectType;
import com.ludot.protocol.Location;
import com.ludot.protocol.PieceDto;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
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

final class PieceNode extends Group {

    private static final double FLASH_MILLIS = 1600;

    private final Circle body;
    private final Circle ring;
    private final Circle halo;
    private final Label badge = new Label();
    private final Label direction = new Label(); // the coin toss result: which way this piece travels
    private final ScaleTransition pulse;
    private Animation motion; // the squash, spin or pop currently playing

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
        styleBadge(radius);
        styleDirectionMarker(radius);
        getChildren().addAll(halo, ring, body, number, badge, direction);
        pulse = new ScaleTransition(Duration.millis(520), ring);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.25);
        pulse.setToY(1.25);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
    }

    // the effect and briefing badge above the disc
    private void styleBadge(double radius) {
        badge.setStyle("-fx-font-size: 11; -fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 6;"
                + "-fx-padding: 0 2;");
        badge.setTranslateX(radius * 0.2);
        badge.setTranslateY(-radius * 2.0);
        badge.setMouseTransparent(true);
    }

    // the little label on the lower right of the disc that shows which way this piece travels
    private void styleDirectionMarker(double radius) {
        direction.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #263238;"
                + "-fx-background-color: rgba(255,255,255,0.92); -fx-background-radius: 8; -fx-padding: 0 3;");
        direction.setTranslateX(radius * 0.3);
        direction.setTranslateY(radius * 0.1);
        direction.setMouseTransparent(true);
        direction.setVisible(false);
    }

    void showState(PieceDto dto, boolean showDirection) {
        direction.setVisible(showDirection && dto.direction() != null);
        if (showDirection && dto.direction() != null) {
            direction.setText(DirectionMark.glyph(dto.direction()));
        }
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

    void land() {
        if (Feedback.get().motionOn()) {
            ScaleTransition squash = new ScaleTransition(Duration.millis(110), this);
            squash.setFromX(1);
            squash.setFromY(1);
            squash.setToX(1.2);
            squash.setToY(0.8);
            squash.setAutoReverse(true);
            squash.setCycleCount(2);
            play(squash);
        }
    }

    void sendHome() {
        if (Feedback.get().motionOn()) {
            RotateTransition spin = new RotateTransition(Duration.millis(260), this);
            spin.setFromAngle(0);
            spin.setByAngle(360);
            ScaleTransition shrink = new ScaleTransition(Duration.millis(130), this);
            shrink.setFromX(1);
            shrink.setFromY(1);
            shrink.setToX(0.55);
            shrink.setToY(0.55);
            shrink.setAutoReverse(true);
            shrink.setCycleCount(2);
            play(new ParallelTransition(spin, shrink));
        }
    }

    void celebrate() {
        flash();
        if (Feedback.get().motionOn()) {
            ScaleTransition pop = new ScaleTransition(Duration.millis(240), this);
            pop.setFromX(1);
            pop.setFromY(1);
            pop.setToX(1.6);
            pop.setToY(1.6);
            pop.setAutoReverse(true);
            pop.setCycleCount(2);
            play(pop);
        }
    }

    // one body animation at a time; a new one replaces the old and every one ends with the piece upright and full size
    private void play(Animation next) {
        if (motion != null) {
            motion.stop();
        }
        resetPose();
        motion = next;
        next.setOnFinished(event -> resetPose());
        next.play();
    }

    private void resetPose() {
        setScaleX(1);
        setScaleY(1);
        setRotate(0);
    }

    // shows a gold halo that fades away, so the eye finds the piece that just moved
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
