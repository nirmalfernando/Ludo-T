package com.ludot.client.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pinger (round-trip timing behind the Connected badge)")
class PingerTest {

    private final AtomicLong nanos = new AtomicLong(5_000_000_000L);
    private final List<Long> reported = new ArrayList<>();

    private Pinger pinger(Pinger.Sender sender) {
        return new Pinger(sender, reported::add, nanos::get);
    }

    @Test @DisplayName("the round trip is the time between the Ping and its Pong, in milliseconds")
    void timesTheRoundTrip() throws IOException {
        Pinger pinger = pinger(() -> { });
        pinger.ping();
        nanos.addAndGet(12_400_000L);
        pinger.pongReceived();
        assertEquals(List.of(12L), reported);
    }

    @Test @DisplayName("a Pong nobody asked for is ignored")
    void unsolicitedPong() {
        pinger(() -> { }).pongReceived();
        assertTrue(reported.isEmpty());
    }

    @Test @DisplayName("only the first Pong after a Ping is timed")
    void secondPongIgnored() throws IOException {
        Pinger pinger = pinger(() -> { });
        pinger.ping();
        nanos.addAndGet(3_000_000L);
        pinger.pongReceived();
        nanos.addAndGet(50_000_000L);
        pinger.pongReceived();
        assertEquals(List.of(3L), reported);
    }

    @Test @DisplayName("each new Ping starts a new measurement")
    void measuresEachPing() throws IOException {
        Pinger pinger = pinger(() -> { });
        pinger.ping();
        nanos.addAndGet(5_000_000L);
        pinger.pongReceived();
        pinger.ping();
        nanos.addAndGet(9_000_000L);
        pinger.pongReceived();
        assertEquals(List.of(5L, 9L), reported);
    }

    @Test @DisplayName("when a Ping cannot be sent the failure callback runs (the link is dead)")
    void sendFailureIsReported() throws InterruptedException {
        CountDownLatch failed = new CountDownLatch(1);
        Pinger pinger = pinger(() -> {
            throw new IOException("closed");
        });
        pinger.start(10_000, failed::countDown);
        assertTrue(failed.await(2, TimeUnit.SECONDS));
        pinger.close();
    }
}
