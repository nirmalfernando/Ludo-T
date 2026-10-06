package com.ludot.client.view;

import com.ludot.client.state.ClientState.Connection;
import com.ludot.protocol.Controller;
import java.util.Optional;

/** What the connection indicators say and which colour their dot is. No JavaFX, so it is easy to test. */
public final class ConnectionPresentation {

    static final String GREEN = "#2eb872";
    static final String AMBER = "#ffb300";
    static final String RED = "#e53935";

    /** Above this round trip the dot turns amber: still connected, but slow. */
    static final long SLOW_MS = 300;

    private ConnectionPresentation() {
    }

    /** "Connected · 12 ms", or just "Connected" before the first answer has come back, "Reconnecting...", or "Offline". */
    public static String statusText(Connection connection, long latencyMs) {
        if (connection == Connection.RECONNECTING) {
            return "Reconnecting...";
        }
        if (connection != Connection.CONNECTED) {
            return "Offline";
        }
        return latencyMs < 0 ? "Connected" : "Connected · " + latencyMs + " ms";
    }

    public static String dotColour(Connection connection, long latencyMs) {
        if (connection == Connection.RECONNECTING) {
            return AMBER;
        }
        if (connection != Connection.CONNECTED) {
            return RED;
        }
        return latencyMs > SLOW_MS ? AMBER : GREEN;
    }

    /** The hover text of the connection badge. */
    public static String tooltip(Connection connection, long latencyMs) {
        return switch (connection) {
            case CONNECTED -> "Your connection to the server (TCP) is open. Round trip: "
                    + (latencyMs < 0 ? "measuring..." : latencyMs + " ms");
            case RECONNECTING -> "The connection was lost: trying to get you back in. Your seat is kept for a short while.";
            case DISCONNECTED -> "The connection to the server is closed";
        };
    }

    /** Dot beside a player on a seat card: green connected, amber away, red offline; none for AI and free seats. */
    public static Optional<String> seatDot(Controller controller) {
        return switch (controller) {
            case HUMAN -> Optional.of(GREEN);
            case AFK -> Optional.of(AMBER);
            case DISCONNECTED -> Optional.of(RED);
            case AI, FREE -> Optional.empty();
        };
    }

    public static String seatDotTip(Controller controller) {
        return switch (controller) {
            case HUMAN -> "Connected";
            case AFK -> "Away: missed turns";
            case DISCONNECTED -> "Offline";
            case AI, FREE -> "";
        };
    }
}
