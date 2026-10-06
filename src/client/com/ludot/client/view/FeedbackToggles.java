package com.ludot.client.view;

import com.ludot.client.feedback.Feedback;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;

/** Two round buttons: sound on or off, and motion effects on or off. Both are remembered between runs. */
public final class FeedbackToggles extends HBox {

    private static final Color ON = Color.web("#3d6fe0");
    private static final Color OFF = Color.web("#a3abd8");
    private static final Color SLASH = Color.web("#d94b4b");

    public FeedbackToggles() {
        super(8);
        setAlignment(Pos.CENTER);
        Feedback feedback = Feedback.get();
        getChildren().addAll(
                toggle(feedback.soundProperty(), "Sound", FeedbackToggles::speaker),
                toggle(feedback.motionProperty(), "Motion effects", FeedbackToggles::sparkle));
    }

    private static ToggleButton toggle(BooleanProperty property, String name, java.util.function.Function<Boolean, Node> icon) {
        ToggleButton button = new ToggleButton();
        button.getStyleClass().add("icon-toggle");
        button.selectedProperty().bindBidirectional(property);
        button.setGraphic(icon.apply(button.isSelected()));
        button.selectedProperty().addListener((obs, old, on) -> {
            button.setGraphic(icon.apply(on));
            button.getTooltip().setText(name + (on ? ": on" : ": off"));
        });
        button.setTooltip(new Tooltip(name + (button.isSelected() ? ": on" : ": off")));
        return button;
    }

    // a loudspeaker with two sound waves; slashed when off
    private static Node speaker(boolean on) {
        Color colour = on ? ON : OFF;
        SVGPath body = new SVGPath();
        body.setContent("M3 9 L7 9 L12 5 L12 19 L7 15 L3 15 Z");
        body.setFill(colour);
        Group icon = new Group(body);
        if (on) {
            icon.getChildren().addAll(stroke("M15 9 Q17.5 12 15 15", colour), stroke("M17.5 6.5 Q22 12 17.5 17.5", colour));
        } else {
            icon.getChildren().add(stroke("M15 8.5 L21 15.5", SLASH));
        }
        return scaled(icon);
    }

    // a four-point sparkle; slashed when off
    private static Node sparkle(boolean on) {
        Color colour = on ? ON : OFF;
        SVGPath star = new SVGPath();
        star.setContent("M12 3 L14 10 L21 12 L14 14 L12 21 L10 14 L3 12 L10 10 Z");
        star.setFill(colour);
        Group icon = new Group(star);
        if (!on) {
            icon.getChildren().add(stroke("M4 4 L20 20", SLASH));
        }
        return scaled(icon);
    }

    private static SVGPath stroke(String path, Color colour) {
        SVGPath line = new SVGPath();
        line.setContent(path);
        line.setFill(Color.TRANSPARENT);
        line.setStroke(colour);
        line.setStrokeWidth(2.2);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    private static Node scaled(Group icon) {
        icon.setScaleX(0.85);
        icon.setScaleY(0.85);
        return icon;
    }
}
