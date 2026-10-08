package com.ludot.client;

import com.ludot.client.view.FinishedGamePresentation;
import com.ludot.protocol.Colour;
import com.ludot.protocol.ReplayEndReason;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("FinishedGamePresentation (the words on the finished-games list and replay screen)")
class FinishedGamePresentationTest {

    @Test @DisplayName("the winner is named by colour")
    void winner() {
        assertEquals("Red won", FinishedGamePresentation.winnerText(Colour.RED));
        assertEquals("Yellow won", FinishedGamePresentation.winnerText(Colour.YELLOW));
    }

    @Test @DisplayName("step counts get thousands separators and the right singular")
    void steps() {
        assertEquals("2,330 steps", FinishedGamePresentation.stepsText(2330));
        assertEquals("12 steps", FinishedGamePresentation.stepsText(12));
        assertEquals("1 step", FinishedGamePresentation.stepsText(1));
    }

    @Test @DisplayName("the date is shown in the viewer's time zone")
    void date() {
        Instant noon = Instant.parse("2026-10-06T12:05:00Z");
        assertEquals("6 Oct 2026, 12:05", FinishedGamePresentation.dateText(noon, ZoneId.of("UTC")));
        assertEquals("6 Oct 2026, 17:35", FinishedGamePresentation.dateText(noon, ZoneId.of("Asia/Colombo")));
    }

    @Test @DisplayName("an unknown finish time says so instead of showing 1970")
    void unknownDate() {
        assertEquals("Date unknown", FinishedGamePresentation.dateText(Instant.EPOCH, ZoneId.of("UTC")));
        assertEquals("Date unknown", FinishedGamePresentation.dateText(null, ZoneId.of("UTC")));
    }

    @Test @DisplayName("progress text and fraction, never outside 0 to 1 and safe when the total is unknown")
    void progress() {
        assertEquals("Step 1,234 of 2,330", FinishedGamePresentation.progressText(1234, 2330));
        assertEquals(0.5, FinishedGamePresentation.progress(50, 100));
        assertEquals(0.0, FinishedGamePresentation.progress(5, 0));
        assertEquals(1.0, FinishedGamePresentation.progress(150, 100));
        assertEquals(0.0, FinishedGamePresentation.progress(-3, 100));
    }

    @Test @DisplayName("each way a replay can end has its own message")
    void endings() {
        assertEquals("Replay finished", FinishedGamePresentation.endText(ReplayEndReason.COMPLETED));
        assertEquals("Replay stopped", FinishedGamePresentation.endText(ReplayEndReason.STOPPED));
        assertNotEquals(FinishedGamePresentation.endText(ReplayEndReason.DIVERGED),
                FinishedGamePresentation.endText(ReplayEndReason.COMPLETED));
    }
}
