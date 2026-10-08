package com.ludot.bot;

import com.ludot.protocol.ServerLink;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Variant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class StressClient {

    private static final int RECENT_REQUESTS = 50;
    private static final long SETUP_TIMEOUT_SECONDS = 10;

    private record Sent(ClientMessage.Request request, CompletableFuture<Response> future) {
    }

    private final StressOptions options;
    private final Consumer<String> log;
    private final StressStats stats;
    private final ConcurrentLinkedDeque<Sent> recent = new ConcurrentLinkedDeque<>();
    private final AtomicInteger recentCount = new AtomicInteger();
    private final AtomicInteger issued = new AtomicInteger();
    private final CountDownLatch done;
    private final StressObserver observer = new StressObserver();
    private final StressRequests requests;

    private ServerLink link;
    private String roomId;

    public StressClient(StressOptions options, Consumer<String> log) {
        this.options = options;
        this.log = log;
        this.stats = new StressStats(options.requests());
        this.done = new CountDownLatch(options.requests());
        this.requests = new StressRequests(options, observer);
    }

    public String roomId() {
        return roomId;
    }

    public StressReport run() throws Exception {
        try (ServerLink connection = ServerLink.connect(options.host(), options.port())) {
            link = connection;
            link.addListener(observer);
            link.startHeartbeat(5_000);
            setUp();
            requests.useRoom(roomId);
            sleepUntil(options.startAtEpochMs());
            if (options.startGame()) {
                expectOk(new ClientMessage.StartGame(UUID.randomUUID(), roomId));
            }
            long begin = System.nanoTime();
            sendAll();
            long limitSeconds = 30 + options.requests() / 20L;
            if (!done.await(limitSeconds, TimeUnit.SECONDS)) {
                log.accept("Gave up waiting for " + done.getCount() + " responses");
            }
            double seconds = (System.nanoTime() - begin) / 1e9;
            playOut(options.waitForGameOverSeconds());
            return report(seconds, finalHashVerdict());
        }
    }

    // ------------------------------------------------------------------ setup

    private void setUp() throws Exception {
        expectOk(new ClientMessage.Hello(UUID.randomUUID(), options.name()));
        if (options.create()) {
            Response created = expectOk(new ClientMessage.CreateRoom(UUID.randomUUID(), RoomMode.PLAY,
                    Variant.LUDO_T, options.randomSeed()));
            roomId = (String) created.payload();
            log.accept("Created room " + roomId);
        } else {
            roomId = options.roomId();
            expectOk(new ClientMessage.JoinRoom(UUID.randomUUID(), roomId));
        }
        Response claim = link.request(new ClientMessage.ClaimSeat(UUID.randomUUID(), roomId, options.seat()))
                .get(SETUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.accept(options.name() + (claim.isOk() ? " took " : " could not take ") + options.seat());
        if (options.create() && options.speed() != null) {
            expectOk(new ClientMessage.SetSpeed(UUID.randomUUID(), roomId, options.speed()));
        }
    }

    private Response expectOk(ClientMessage.Request request) throws Exception {
        Response response = link.request(request).get(SETUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!response.isOk()) {
            throw new IllegalStateException(request.getClass().getSimpleName() + " refused: "
                    + response.errorCode() + " " + response.message());
        }
        return response;
    }

    private static void sleepUntil(long epochMs) throws InterruptedException {
        long wait = epochMs - System.currentTimeMillis();
        if (wait > 0) {
            Thread.sleep(wait);
        }
    }

    // ------------------------------------------------------------------ sending

    private void sendAll() throws InterruptedException {
        long intervalNanos = options.ratePerSecond() > 0
                ? TimeUnit.SECONDS.toNanos(1) * options.threads() / options.ratePerSecond() : 0;
        Thread[] senders = new Thread[options.threads()];
        for (int i = 0; i < senders.length; i++) {
            senders[i] = new Thread(() -> senderLoop(intervalNanos), "stress-sender-" + i);
            senders[i].setDaemon(true);
            senders[i].start();
        }
        for (Thread sender : senders) {
            sender.join();
        }
    }

    private void senderLoop(long intervalNanos) {
        long next = System.nanoTime();
        while (true) {
            int n = issued.getAndIncrement();
            if (n >= options.requests()) {
                return;
            }
            if (intervalNanos > 0) {
                next += intervalNanos;
                long wait = next - System.nanoTime();
                if (wait > 0) {
                    LockSupportSleep.nanos(wait);
                }
            }
            issue(n);
        }
    }

    // a real turn takes priority over the random mix, so the game keeps moving during the flood
    private void issue(int n) {
        ServerMessage.YourTurn turn = observer.takeTurn();
        if (turn != null) {
            track(n, RequestKind.LEGIT, requests.legitMove(turn));
            return;
        }
        RequestKind kind = RequestKind.pick(ThreadLocalRandom.current().nextDouble());
        if (kind == RequestKind.PAUSE_NOT_HOST && observer.isHost()) {
            kind = RequestKind.SNAPSHOT; // the host's pause would really pause the game
        }
        if (kind == RequestKind.DUPLICATE) {
            duplicate(n);
        } else {
            track(n, kind, requests.of(kind));
        }
    }

    // resends an earlier request under the same id: the server must answer with the original response
    private void duplicate(int n) {
        Object[] candidates = recent.toArray();
        if (candidates.length == 0) {
            track(n, RequestKind.SNAPSHOT, requests.of(RequestKind.SNAPSHOT));
            return;
        }
        Sent original = (Sent) candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
        long start = System.nanoTime();
        CompletableFuture<Response> repeat = link.request(original.request());
        repeat.whenComplete((response, error) -> {
            stats.latency(n, System.nanoTime() - start);
            judgeDuplicate(original, response, error);
            done.countDown();
        });
    }

    private void judgeDuplicate(Sent original, Response repeat, Throwable error) {
        if (error != null) {
            countFailure(error, "duplicate");
            return;
        }
        stats.response();
        Response first = original.future().getNow(null);
        if (first == null) {
            stats.duplicateReplayed(); // the original was still unanswered; a repeat can only come later
        } else if (first.equals(repeat)) {
            stats.duplicateReplayed();
        } else {
            stats.unexpected("duplicate answered differently: " + first + " then " + repeat);
        }
        stats.outcome("DUPLICATE(replayed)");
    }

    private void track(int n, RequestKind kind, ClientMessage.Request request) {
        long start = System.nanoTime();
        CompletableFuture<Response> future = link.request(request);
        remember(request, future);
        future.whenComplete((response, error) -> {
            stats.latency(n, System.nanoTime() - start);
            if (error != null) {
                countFailure(error, kind.name());
            } else {
                judge(kind, request, response);
            }
            done.countDown();
        });
    }

    private void remember(ClientMessage.Request request, CompletableFuture<Response> future) {
        boolean roomLevel = request instanceof ClientMessage.RoomRequest || request instanceof ClientMessage.SendChat;
        if (!roomLevel) {
            return;
        }
        recent.addLast(new Sent(request, future));
        if (recentCount.incrementAndGet() > RECENT_REQUESTS) {
            recent.pollFirst();
            recentCount.decrementAndGet();
        }
    }

    private void judge(RequestKind kind, ClientMessage.Request request, Response response) {
        stats.response();
        ErrorCode code = response.errorCode();
        stats.outcome(response.isOk() ? "OK" : code.name());
        if (request instanceof ClientMessage.SendChat) {
            stats.chat(response.isOk());
        }
        if (!kind.accepts(response.isOk(), code)) {
            stats.unexpected(kind + " got " + (response.isOk() ? "OK" : code + " (" + response.message() + ")"));
        }
    }

    private void countFailure(Throwable error, String what) {
        Throwable cause = error.getCause() != null && error instanceof java.util.concurrent.CompletionException
                ? error.getCause() : error;
        if (cause instanceof TimeoutException) {
            stats.timeout();
        } else {
            stats.unexpected(what + " failed: " + cause);
        }
    }

    // ------------------------------------------------------------------ after the flood

    // keeps answering our seat's turns so the game can reach its end
    private void playOut(int waitSeconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(waitSeconds);
        while (!observer.finished() && !link.isClosed() && System.nanoTime() < deadline) {
            ServerMessage.YourTurn turn = observer.takeTurn();
            if (turn != null) {
                link.request(requests.legitMove(turn));
            } else {
                Thread.sleep(2);
            }
        }
    }

    private String finalHashVerdict() throws Exception {
        if (!observer.finished() || !observer.sawUpdate()) {
            return "n/a (game had not finished)";
        }
        ServerMessage.RoomSnapshot before = observer.lastSnapshot();
        expectOk(new ClientMessage.RequestSnapshot(UUID.randomUUID(), roomId));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SETUP_TIMEOUT_SECONDS);
        while (observer.lastSnapshot() == before && System.nanoTime() < deadline) {
            Thread.sleep(2);
        }
        ServerMessage.RoomSnapshot snapshot = observer.lastSnapshot();
        boolean match = snapshot != null && snapshot != before && snapshot.stateHash() == observer.lastUpdateHash();
        return String.format("client=%08X server=%s  %s", observer.lastUpdateHash(),
                snapshot == null ? "?" : String.format("%08X", snapshot.stateHash()), match ? "MATCH" : "MISMATCH");
    }

    private StressReport report(double seconds, String finalHash) {
        return new StressReport(options.name(), options.requests(), stats.responseCount(), stats.timeoutCount(),
                seconds, stats.outcomeCounts(), stats.unexpectedCount(), stats.unexpectedSamples(),
                stats.percentileMs(50), stats.percentileMs(95), stats.maxMs(), stats.chatSentCount(),
                stats.chatAcceptedCount(), stats.chatRateLimitedCount(), observer.chatOrderHash(), observer.chatLines(),
                observer.updatesSeen(), observer.hashMismatches(), finalHash);
    }

    // tiny helper so the sender loop reads cleanly
    private static final class LockSupportSleep {
        private LockSupportSleep() {
        }

        static void nanos(long nanos) {
            java.util.concurrent.locks.LockSupport.parkNanos(nanos);
        }
    }
}
