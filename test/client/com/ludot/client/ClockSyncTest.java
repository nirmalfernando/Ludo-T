package com.ludot.client;

import com.ludot.client.state.ClockSync;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ClockSync")
class ClockSyncTest {

    @Test @DisplayName("with equal clocks the time left is simply the deadline minus now")
    void sameClock() {
        ClockSync sync = new ClockSync();
        sync.update(1_000_000, 1_000_000);
        assertEquals(30_000, sync.remainingMs(1_030_000, 1_000_000));
    }

    @Test @DisplayName("a client clock that runs ahead of the server still shows the right time left")
    void clientAhead() {
        ClockSync sync = new ClockSync();
        sync.update(1_000_000, 1_090_000); // we read 90 s later than the server
        // the server says the deadline is 30 s after its 1,000,000; locally that is 1,120,000
        assertEquals(30_000, sync.remainingMs(1_030_000, 1_090_000));
        assertEquals(10_000, sync.remainingMs(1_030_000, 1_110_000));
    }

    @Test @DisplayName("a client clock that runs behind the server still shows the right time left")
    void clientBehind() {
        ClockSync sync = new ClockSync();
        sync.update(5_000_000, 4_000_000);
        assertEquals(30_000, sync.remainingMs(5_030_000, 4_000_000));
    }

    @Test @DisplayName("the time left goes negative after the deadline but the text never does")
    void pastDeadline() {
        ClockSync sync = new ClockSync();
        sync.update(0, 0);
        assertEquals(-2_000, sync.remainingMs(10_000, 12_000));
        assertEquals("0:00", ClockSync.format(-2_000));
    }

    @Test @DisplayName("countdown text rounds up so it never shows 0:00 while time remains")
    void formatting() {
        assertEquals("0:30", ClockSync.format(30_000));
        assertEquals("0:23", ClockSync.format(22_100));
        assertEquals("0:01", ClockSync.format(1));
        assertEquals("1:05", ClockSync.format(65_000));
        assertEquals("0:00", ClockSync.format(0));
    }
}
