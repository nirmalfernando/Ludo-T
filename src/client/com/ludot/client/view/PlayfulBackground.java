package com.ludot.client.view;

import com.ludot.protocol.Colour;
import java.util.Random;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The backdrop of the connect screen and the lobby: a soft gradient, the four board-base colours as huge pale
 * blocks in the corners, and a few faint dice and pawns drifting about. It reads as "Ludo" without competing
 * with the content on top.
 */
final class PlayfulBackground extends Pane {

    private static final double FAINT = 0.16;

    PlayfulBackground(long seed) {
        setStyle("-fx-background-color: linear-gradient(to bottom right, #f4f7ff, #e8efff);");
        setMouseTransparent(true);
        getChildren().add(corner(Colour.RED, 0, 0));
        getChildren().add(corner(Colour.GREEN, 1, 0));
        getChildren().add(corner(Colour.BLUE, 0, 1));
        getChildren().add(corner(Colour.YELLOW, 1, 1));
        Random random = new Random(seed);
        for (int i = 0; i < 7; i++) {
            addDrifter(random, i);
        }
    }

    // a base-sized block hugging a corner, partly off screen; (0,0) is top-left, (1,1) bottom-right
    private Rectangle corner(Colour colour, int right, int bottom) {
        Rectangle block = new Rectangle();
        block.setArcWidth(90);
        block.setArcHeight(90);
        block.setFill(Theme.strong(colour).deriveColor(0, 0.8, 1.1, 0.2));
        block.widthProperty().bind(widthProperty().multiply(0.26));
        block.heightProperty().bind(heightProperty().multiply(0.34));
        block.layoutXProperty().bind(widthProperty().multiply(right).subtract(block.widthProperty().multiply(right * 0.75 + 0.25)));
        block.layoutYProperty().bind(heightProperty().multiply(bottom).subtract(block.heightProperty().multiply(bottom * 0.75 + 0.25)));
        return block;
    }

    private void addDrifter(Random random, int index) {
        Node shape = index % 2 == 0
                ? new DiceDecor(1 + random.nextInt(6), 46 + random.nextInt(30), Color.web("#9aa5d8"))
                : new PawnMascot(Colour.values()[random.nextInt(4)], 70 + random.nextInt(30));
        shape.setOpacity(FAINT);
        shape.setRotate(random.nextInt(50) - 25);
        double fromX = random.nextDouble();
        double fromY = random.nextDouble();
        shape.layoutXProperty().bind(widthProperty().multiply(fromX));
        shape.layoutYProperty().bind(heightProperty().multiply(fromY));
        getChildren().add(shape);
        drift(shape, random);
    }

    private static void drift(Node shape, Random random) {
        TranslateTransition move = new TranslateTransition(Duration.seconds(9 + random.nextInt(7)), shape);
        move.setByX(random.nextInt(80) - 40);
        move.setByY(random.nextInt(80) - 40);
        move.setAutoReverse(true);
        move.setCycleCount(TranslateTransition.INDEFINITE);
        move.setInterpolator(Interpolator.EASE_BOTH);
        RotateTransition turn = new RotateTransition(Duration.seconds(12 + random.nextInt(8)), shape);
        turn.setByAngle(random.nextInt(40) - 20);
        turn.setAutoReverse(true);
        turn.setCycleCount(RotateTransition.INDEFINITE);
        Motion.whileShowing(shape, move);
        Motion.whileShowing(shape, turn);
    }
}
