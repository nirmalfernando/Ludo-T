package com.ludot.client.view;

import com.ludot.protocol.Colour;
import com.ludot.protocol.ReplayEndReason;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The words used on the finished-games list and the replay screen. No JavaFX, so it is easy to test. */
public final class FinishedGamePresentation {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private FinishedGamePresentation() {
    }

    public static String winnerText(Colour winner) {
        return Theme.display(winner) + " won";
    }

    /** "2,330 steps": the length of the stored game (every roll and every move is a step). */
    public static String stepsText(int steps) {
        return String.format(Locale.ENGLISH, "%,d %s", steps, steps == 1 ? "step" : "steps");
    }

    /** When the game finished, in the viewer's time zone; "Date unknown" if the server did not know. */
    public static String dateText(Instant finished, ZoneId zone) {
        return finished == null || finished.equals(Instant.EPOCH) ? "Date unknown" : DATE.withZone(zone).format(finished);
    }

    public static String progressText(int index, int total) {
        return String.format(Locale.ENGLISH, "Step %,d of %,d", index, total);
    }

    /** How far through the replay, from 0 to 1. */
    public static double progress(int index, int total) {
        return total <= 0 ? 0 : Math.max(0, Math.min(1, index / (double) total));
    }

    public static String endText(ReplayEndReason reason) {
        return switch (reason) {
            case COMPLETED -> "Replay finished";
            case STOPPED -> "Replay stopped";
            case DIVERGED -> "This replay no longer matches the stored game";
        };
    }
}
