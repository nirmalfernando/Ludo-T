package com.ludot.client;

import com.ludot.client.state.ClientState.Connection;
import com.ludot.client.view.ConnectionPresentation;
import com.ludot.protocol.Controller;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConnectionPresentation (the green dot and its text)")
class ConnectionPresentationTest {

    @Test @DisplayName("connected shows the round trip, or just 'Connected' before the first answer")
    void connectedText() {
        assertEquals("Connected · 12 ms", ConnectionPresentation.statusText(Connection.CONNECTED, 12));
        assertEquals("Connected · 0 ms", ConnectionPresentation.statusText(Connection.CONNECTED, 0));
        assertEquals("Connected", ConnectionPresentation.statusText(Connection.CONNECTED, -1));
    }

    @Test @DisplayName("a closed connection says Offline whatever the last round trip was")
    void offlineText() {
        assertEquals("Offline", ConnectionPresentation.statusText(Connection.DISCONNECTED, 12));
    }

    @Test @DisplayName("the dot is green when healthy, amber when slow, red when offline")
    void dotColours() {
        assertEquals("#2eb872", ConnectionPresentation.dotColour(Connection.CONNECTED, 20));
        assertEquals("#2eb872", ConnectionPresentation.dotColour(Connection.CONNECTED, -1));
        assertEquals("#2eb872", ConnectionPresentation.dotColour(Connection.CONNECTED, 300));
        assertEquals("#ffb300", ConnectionPresentation.dotColour(Connection.CONNECTED, 301));
        assertEquals("#e53935", ConnectionPresentation.dotColour(Connection.DISCONNECTED, 5));
    }

    @Test @DisplayName("a seated person gets green (connected), amber (away) or red (offline); AI and free seats get no dot")
    void seatDots() {
        assertEquals(Optional.of("#2eb872"), ConnectionPresentation.seatDot(Controller.HUMAN));
        assertEquals(Optional.of("#ffb300"), ConnectionPresentation.seatDot(Controller.AFK));
        assertEquals(Optional.of("#e53935"), ConnectionPresentation.seatDot(Controller.DISCONNECTED));
        assertTrue(ConnectionPresentation.seatDot(Controller.AI).isEmpty());
        assertTrue(ConnectionPresentation.seatDot(Controller.FREE).isEmpty());
    }

    @Test @DisplayName("each dot has a tooltip that says what it means")
    void seatTips() {
        assertEquals("Connected", ConnectionPresentation.seatDotTip(Controller.HUMAN));
        assertEquals("Offline", ConnectionPresentation.seatDotTip(Controller.DISCONNECTED));
        assertTrue(ConnectionPresentation.seatDotTip(Controller.AFK).startsWith("Away"));
    }
}
