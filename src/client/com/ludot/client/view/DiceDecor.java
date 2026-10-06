package com.ludot.client.view;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/** A rounded die with pips, used as decoration (the lobby background and the title). */
final class DiceDecor extends Group {

    // pip centres on a 3x3 grid, as fractions of the die's side, for each face value
    private static final double[][][] PIPS = {
        {},
        {{0.5, 0.5}},
        {{0.28, 0.28}, {0.72, 0.72}},
        {{0.25, 0.25}, {0.5, 0.5}, {0.75, 0.75}},
        {{0.28, 0.28}, {0.72, 0.28}, {0.28, 0.72}, {0.72, 0.72}},
        {{0.27, 0.27}, {0.73, 0.27}, {0.5, 0.5}, {0.27, 0.73}, {0.73, 0.73}},
        {{0.28, 0.24}, {0.72, 0.24}, {0.28, 0.5}, {0.72, 0.5}, {0.28, 0.76}, {0.72, 0.76}},
    };

    DiceDecor(int face, double side, Color pip) {
        Rectangle die = new Rectangle(side, side, Color.WHITE);
        die.setArcWidth(side * 0.3);
        die.setArcHeight(side * 0.3);
        die.setStroke(Color.web("#c3cae6"));
        die.setStrokeWidth(Math.max(2, side * 0.05));
        getChildren().add(die);
        for (double[] at : PIPS[face]) {
            getChildren().add(new Circle(at[0] * side, at[1] * side, side * 0.085, pip));
        }
    }
}
