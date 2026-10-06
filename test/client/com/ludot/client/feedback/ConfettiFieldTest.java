package com.ludot.client.feedback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConfettiField (the falling confetti)")
class ConfettiFieldTest {

    private static final double WIDTH = 600;
    private static final double HEIGHT = 600;

    @Test @DisplayName("every piece starts above the top edge and inside the width")
    void startsAboveTheTop() {
        ConfettiField field = new ConfettiField(100, WIDTH, 1);
        assertEquals(100, field.count());
        for (int i = 0; i < field.count(); i++) {
            assertTrue(field.y(i) < 0, "piece " + i + " should start above the top");
            assertTrue(field.x(i) >= 0 && field.x(i) <= WIDTH, "piece " + i + " should start within the width");
            assertTrue(field.colour(i) >= 0 && field.colour(i) < 6);
            assertTrue(field.size(i) > 0);
        }
    }

    @Test @DisplayName("the same seed gives the same burst, a different seed a different one")
    void seededAndRepeatable() {
        ConfettiField a = new ConfettiField(50, WIDTH, 7);
        ConfettiField b = new ConfettiField(50, WIDTH, 7);
        ConfettiField c = new ConfettiField(50, WIDTH, 8);
        for (int step = 0; step < 30; step++) {
            a.step(1 / 60.0);
            b.step(1 / 60.0);
            c.step(1 / 60.0);
        }
        for (int i = 0; i < a.count(); i++) {
            assertEquals(a.x(i), b.x(i));
            assertEquals(a.y(i), b.y(i));
        }
        assertNotEquals(a.x(0), c.x(0));
    }

    @Test @DisplayName("pieces fall: after a second every piece is lower than it started")
    void fallsDown() {
        ConfettiField field = new ConfettiField(40, WIDTH, 3);
        double[] before = new double[field.count()];
        for (int i = 0; i < before.length; i++) {
            before[i] = field.y(i);
        }
        for (int step = 0; step < 60; step++) {
            field.step(1 / 60.0);
        }
        for (int i = 0; i < before.length; i++) {
            assertTrue(field.y(i) > before[i] + 100, "piece " + i + " barely moved");
        }
    }

    @Test @DisplayName("a burst is not finished at first and ends by itself once everything has fallen past the bottom")
    void finishesByItself() {
        ConfettiField field = new ConfettiField(120, WIDTH, 5);
        assertFalse(field.finished(HEIGHT));
        int steps = 0;
        while (!field.finished(HEIGHT) && steps < 60 * 30) {
            field.step(1 / 60.0);
            steps++;
        }
        assertTrue(field.finished(HEIGHT), "confetti must end, or the animation would run forever");
        assertTrue(steps < 60 * 10, "a burst should be over within ten seconds, took " + steps / 60.0 + " s");
    }

    @Test @DisplayName("spinning pieces keep turning while they fall")
    void piecesSpin() {
        ConfettiField field = new ConfettiField(30, WIDTH, 11);
        double before = field.angle(0);
        field.step(0.5);
        assertNotEquals(before, field.angle(0));
    }
}
