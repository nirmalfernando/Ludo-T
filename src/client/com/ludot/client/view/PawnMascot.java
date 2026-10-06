package com.ludot.client.view;

import com.ludot.protocol.Colour;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Path;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.ClosePath;
import javafx.util.Duration;

/**
 * A friendly Ludo pawn drawn from shapes: a round head with a glossy highlight on a bell-shaped body and a flat
 * base. It is the mascot of the connect screen and the lobby, and shows who sits where on a room card.
 */
final class PawnMascot extends Group {

    private static final double BOB_HEIGHT = 7;

    /** @param size height in pixels; the pawn is about 0.75 times as wide */
    PawnMascot(Colour colour, double size) {
        this(Theme.strong(colour), size);
    }

    PawnMascot(Color main, double size) {
        double u = size / 100;
        Color dark = main.deriveColor(0, 1, 0.72, 1);
        Color light = main.deriveColor(0, 0.6, 1.25, 1);

        Ellipse shadow = new Ellipse(0, 95 * u, 30 * u, 5 * u);
        shadow.setFill(Color.rgb(43, 51, 102, 0.18));
        Path body = bellBody(u);
        body.setFill(main);
        body.setStroke(dark);
        body.setStrokeWidth(3 * u);
        Ellipse base = new Ellipse(0, 88 * u, 31 * u, 8 * u);
        base.setFill(dark);
        Circle head = new Circle(0, 24 * u, 21 * u, main);
        head.setStroke(dark);
        head.setStrokeWidth(3 * u);
        Ellipse gloss = new Ellipse(-7 * u, 16 * u, 6 * u, 9 * u);
        gloss.setFill(light);
        gloss.setRotate(-30);
        gloss.setOpacity(0.85);
        getChildren().addAll(shadow, base, body, head, gloss);
    }

    // a bell: narrow under the head, flaring to the base
    private static Path bellBody(double u) {
        return new Path(
                new MoveTo(-11 * u, 40 * u),
                new CubicCurveTo(-12 * u, 62 * u, -34 * u, 66 * u, -31 * u, 88 * u),
                new LineTo(31 * u, 88 * u),
                new CubicCurveTo(34 * u, 66 * u, 12 * u, 62 * u, 11 * u, 40 * u),
                new ClosePath());
    }

    /** Makes the pawn bob up and down; {@code offsetMillis} staggers a row of pawns so they do not move in step. */
    PawnMascot bob(double offsetMillis) {
        TranslateTransition up = new TranslateTransition(Duration.millis(900), this);
        up.setFromY(0);
        up.setToY(-BOB_HEIGHT);
        up.setAutoReverse(true);
        up.setCycleCount(TranslateTransition.INDEFINITE);
        up.setInterpolator(Interpolator.EASE_BOTH);
        up.setDelay(Duration.millis(offsetMillis));
        Motion.whileShowing(this, up);
        return this;
    }
}
