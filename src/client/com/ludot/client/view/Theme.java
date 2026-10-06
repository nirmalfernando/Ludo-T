package com.ludot.client.view;

import com.ludot.protocol.Colour;
import java.io.IOException;
import java.io.InputStream;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** The few colours and styles the whole UI shares. */
public final class Theme {

    public static final Color BOARD_BACKGROUND = Color.web("#fafafa");
    public static final Color GRID_LINE = Color.web("#b0bec5");
    public static final Color HIGHLIGHT = Color.web("#ffb300");

    /** Style class carried by the screens that use the friendly look (see playful.css). */
    public static final String PLAYFUL = "playful";

    private static final String STYLESHEET = "/playful.css";
    private static final String DISPLAY_FONT = "/fonts/LilitaOne-Regular.ttf";

    private Theme() {
    }

    /** Loads the rounded display font so CSS can use it; a missing font only means the fallback font is used. */
    public static void loadFonts() {
        try (InputStream in = Theme.class.getResourceAsStream(DISPLAY_FONT)) {
            if (in != null) {
                Font.loadFont(in, 12);
            }
        } catch (IOException e) {
            // the stylesheet names fallback fonts, so there is nothing to do
        }
    }

    /** Gives a screen (or dialog root) the friendly look; the rules are scoped, so nothing else changes. */
    public static void applyPlayful(Parent root) {
        root.getStyleClass().add(PLAYFUL);
        var url = Theme.class.getResource(STYLESHEET);
        if (url != null) {
            root.getStylesheets().add(url.toExternalForm());
        }
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
