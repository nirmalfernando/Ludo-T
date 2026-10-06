package com.ludot.client.view;

import com.ludot.protocol.Colour;
import javafx.scene.paint.Color;

/** The few colours and styles the whole UI shares. */
public final class Theme {

    public static final Color BOARD_BACKGROUND = Color.web("#fafafa");
    public static final Color GRID_LINE = Color.web("#b0bec5");
    public static final Color HIGHLIGHT = Color.web("#ffb300");
    public static final String PANEL_STYLE = "-fx-background-color: #f5f7fa; -fx-padding: 10;";
    public static final String CARD_STYLE = "-fx-background-color: white; -fx-border-color: #d5dbe1; "
            + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;";

    private Theme() {
    }

    /** Strong colour for pieces and home straights. */
    public static Color strong(Colour colour) {
        return switch (colour) {
            case RED -> Color.web("#e53935");
            case GREEN -> Color.web("#43a047");
            case YELLOW -> Color.web("#fbc02d");
            case BLUE -> Color.web("#1e88e5");
        };
    }

    /** Pale tint for bases and start cells. */
    public static Color tint(Colour colour) {
        return strong(colour).deriveColor(0, 0.35, 1.25, 1);
    }

    public static String hex(Colour colour) {
        Color c = strong(colour);
        return String.format("#%02x%02x%02x", (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }

    public static String display(Colour colour) {
        String name = colour.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
