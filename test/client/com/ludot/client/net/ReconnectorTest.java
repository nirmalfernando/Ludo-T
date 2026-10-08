package com.ludot.client.net;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Reconnector (when and how often to retry)")
class ReconnectorTest {

    private final AtomicLong now = new AtomicLong(0);
    private final List<Long> sleeps = new ArrayList<>();

    private Reconnector<String> reconnector(long[] backoff, long giveUpAfter) {
        return new Reconnector<>(backoff, giveUpAfter, millis -> {
            sleeps.add(millis);
            now.addAndGet(millis);
        }, now::get);
    }

    @Test @DisplayName("a first try that works returns at once without waiting")
    void worksFirstTime() {
        var result = reconnector(new long[] {1_000}, 40_000).run(() -> "session");
        assertTrue(result.succeeded());
        assertEquals("session", result.value());
        assertTrue(sleeps.isEmpty());
    }

    @Test @DisplayName("it waits 1 s, then 2 s, between failed tries and returns the first one that works")
    void backsOffThenSucceeds() {
        AtomicInteger tries = new AtomicInteger();
        var result = reconnector(new long[] {1_000, 2_000, 4_000, 8_000}, 40_000).run(() -> {
            if (tries.incrementAndGet() < 3) {
                throw new java.io.IOException("refused");
            }
            return "session";
        });
        assertTrue(result.succeeded());
        assertEquals(List.of(1_000L, 2_000L), sleeps);
        assertEquals(3, tries.get());
    }

    @Test @DisplayName("the wait stops growing at the last value of the schedule")
    void backoffIsCapped() {
        AtomicInteger tries = new AtomicInteger();
        reconnector(new long[] {1_000, 2_000, 4_000}, 40_000).run(() -> {
            if (tries.incrementAndGet() < 7) {
                throw new java.io.IOException("refused");
            }
            return "session";
        });
        assertEquals(List.of(1_000L, 2_000L, 4_000L, 4_000L, 4_000L, 4_000L), sleeps);
    }

    @Test @DisplayName("when the server no longer knows the session it stops at once: retrying cannot help")
    void expiredSessionStopsImmediately() {
        var result = reconnector(new long[] {1_000}, 40_000).run(() -> {
            throw new SessionExpiredException("gone");
        });
        assertFalse(result.succeeded());
        assertTrue(result.failure().contains("expired"));
        assertTrue(sleeps.isEmpty());
    }

    @Test @DisplayName("it gives up once the next wait would run past the time limit, and says why")
    void givesUpWhenTimeIsUp() {
        var result = reconnector(new long[] {1_000}, 3_500).run(() -> {
            throw new java.io.IOException("boom");
        });
        assertFalse(result.succeeded());
        assertEquals(List.of(1_000L, 1_000L, 1_000L), sleeps);
        assertTrue(result.failure().contains("Could not reconnect"));
        assertTrue(result.failure().contains("boom"));
    }

    @Test @DisplayName("cancelling stops it, for example when the person signs out")
    void cancel() {
        Reconnector<String> reconnector = reconnector(new long[] {1_000}, 40_000);
        AtomicInteger tries = new AtomicInteger();
        var result = reconnector.run(() -> {
            tries.incrementAndGet();
            reconnector.cancel();
            throw new java.io.IOException("refused");
        });
        assertFalse(result.succeeded());
        assertEquals(1, tries.get());
        assertTrue(result.failure().contains("cancelled"));
    }

    @Test @DisplayName("an interrupted wait ends the attempt instead of hanging")
    void interrupted() {
        Reconnector<String> interrupting = new Reconnector<>(new long[] {1_000}, 40_000, millis -> {
            throw new InterruptedException();
        }, now::get);
        var result = interrupting.run(() -> {
            throw new java.io.IOException("refused");
        });
        assertFalse(result.succeeded());
        assertTrue(Thread.interrupted(), "the interrupt flag should be restored");
    }
}
