package com.ludot.bot;

import com.ludot.protocol.ServerLink;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.StateHash;
import com.ludot.protocol.Variant;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.CRC32;

/**
 * Floods a room with asynchronous requests: several sender threads pipeline requests without waiting
 * for answers, a single reader thread completes them, and every answer is judged against what that kind
 * of request should produce. Real turns are answered when our seat's turn arrives; everything else is
 * deliberately wrong (stale, out of turn, duplicate, bad index, not host) to exercise the server's
 * validation, its request queue and its duplicate handling at speed.
 */
public final class StressClient {

    private static final int RECENT_REQUESTS = 50;
    private static final int BAD_INDEX = 99;
    private static final long SETUP_TIMEOUT_SECONDS = 10;

    private record Sent(ClientMessage.Request request, CompletableFuture<Response> future) {
    }

    private final StressOptions options;
    private final Consumer<String> log;
    private final StressStats stats;
    private final ConcurrentLinkedDeque<Sent> recent = new ConcurrentLinkedDeque<>();
    private final AtomicInteger recentCount = new AtomicInteger();
    private final AtomicInteger issued = new AtomicInteger();
    private final AtomicInteger chatCounter = new AtomicInteger();
    private final CountDownLatch done;
    private final BotStrategy strategy;

    // observed by the reader thread, read by the senders
    private final AtomicReference<ServerMessage.YourTurn> ourTurn = new AtomicReference<>();
    private volatile int latestTurn;
    private volatile String myId = "";
    private volatile String hostId = "";
    private volatile boolean finished;
    private volatile long lastUpdateHash;
    private volatile boolean sawUpdate;
    private final AtomicLong updatesSeen = new AtomicLong();
    private final AtomicLong hashMismatches = new AtomicLong();
    private final AtomicReference<ServerMessage.RoomSnapshot> lastSnapshot = new AtomicReference<>();
    private final CRC32 chatCrc = new CRC32();     // touched only by the reader thread
    private final AtomicLong chatOrderHash = new AtomicLong();
    private final AtomicLong chatLines = new AtomicLong();

    private ServerLink link;
    private String roomId;

    public StressClient(StressOptions options, Consumer<String> log) {
        this.options = options;
        this.log = log;
        this.stats = new StressStats(options.requests());
        this.done = new CountDownLatch(options.requests());
        this.strategy = BotStrategies.forColour(options.seat(), new Random(options.randomSeed()));
    }

    /** The room being stressed; known once {@link #run} has set up (it may have been created by this client). */
    public String roomId() {
        return roomId;
    }

    public StressReport run() throws Exception {
        try (ServerLink connection = ServerLink.connect(options.host(), options.port())) {
            link = connection;
            link.addListener(this::onMessage);
            link.startHeartbeat(5_000);
            setUp();
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
        ServerMessage.YourTurn turn = ourTurn.getAndSet(null);
        if (turn != null) {
            track(n, RequestKind.LEGIT, legitMove(turn));
            return;
        }
        RequestKind kind = RequestKind.pick(ThreadLocalRandom.current().nextDouble());
        if (kind == RequestKind.PAUSE_NOT_HOST && myId.equals(hostId)) {
            kind = RequestKind.SNAPSHOT; // the host's pause would really pause the game
        }
        if (kind == RequestKind.DUPLICATE) {
            duplicate(n);
        } else {
            track(n, kind, build(kind));
        }
    }

    private ClientMessage.Request legitMove(ServerMessage.YourTurn turn) {
        return turn.phase() == Phase.AWAITING_ROLL
                ? new ClientMessage.Roll(UUID.randomUUID(), roomId, turn.turnNumber())
                : new ClientMessage.ChooseMove(UUID.randomUUID(), roomId, turn.turnNumber(),
                        strategy.choose(turn.options()));
    }

    private ClientMessage.Request build(RequestKind kind) {
        UUID id = UUID.randomUUID();
        return switch (kind) {
            // our view lags the server's, so latestTurn - 5 is always behind the real turn (even if negative)
            case STALE_MOVE -> new ClientMessage.ChooseMove(id, roomId, latestTurn - 5, 0);
            case OUT_OF_TURN_ROLL -> new ClientMessage.Roll(id, roomId, latestTurn);
            case LIST_ROOMS -> new ClientMessage.ListRooms(id);
            case CHAT -> new ClientMessage.SendChat(id, roomId, options.name() + "#" + chatCounter.getAndIncrement());
            case CLAIM_TAKEN -> new ClientMessage.ClaimSeat(id, roomId, options.seat());
            case PAUSE_NOT_HOST -> new ClientMessage.PauseGame(id, roomId);
            case BAD_INDEX -> new ClientMessage.ChooseMove(id, roomId, latestTurn, BAD_INDEX);
            default -> new ClientMessage.RequestSnapshot(id, roomId);
        };
    }

    // resends an earlier request under the same id: the server must answer with the original response
    private void duplicate(int n) {
        Object[] candidates = recent.toArray();
        if (candidates.length == 0) {
            track(n, RequestKind.SNAPSHOT, build(RequestKind.SNAPSHOT));
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

    // ------------------------------------------------------------------ what the reader sees

    private void onMessage(ServerMessage message) {
        if (message instanceof ServerMessage.Welcome welcome) {
            myId = welcome.playerId();
        } else if (message instanceof ServerMessage.YourTurn turn) {
            latestTurn = Math.max(latestTurn, turn.turnNumber());
            ourTurn.set(turn);
        } else if (message instanceof ServerMessage.StateUpdate update) {
            onUpdate(update);
        } else if (message instanceof ServerMessage.RoomSnapshot snapshot) {
            hostId = snapshot.hostId();
            lastSnapshot.set(snapshot);
        } else if (message instanceof ServerMessage.RoomStateChanged changed) {
            hostId = changed.hostId();
        } else if (message instanceof ServerMessage.ChatMessage chat) {
            onChat(chat);
        } else if (message instanceof ServerMessage.GameOver) {
            finished = true;
        }
    }

    private void onUpdate(ServerMessage.StateUpdate update) {
        int turn = update.turn().turnNumber();
        latestTurn = Math.max(latestTurn, turn);
        updatesSeen.incrementAndGet();
        if (StateHash.of(update.board(), turn) != update.stateHash()) {
            hashMismatches.incrementAndGet();
        }
        lastUpdateHash = update.stateHash();
        sawUpdate = true;
        ServerMessage.YourTurn pending = ourTurn.get();
        if (pending != null && pending.turnNumber() < turn) {
            ourTurn.compareAndSet(pending, null); // the game moved past it (the AI took over, or we were too slow)
        }
    }

    private void onChat(ServerMessage.ChatMessage chat) {
        if (chat.roomId() == null || chat.system()) {
            return;
        }
        chatCrc.update((chat.seq() + "|" + chat.senderId() + "|" + chat.text() + "\n").getBytes(StandardCharsets.UTF_8));
        chatOrderHash.set(chatCrc.getValue());
        chatLines.incrementAndGet();
    }

    // ------------------------------------------------------------------ after the flood

    // keeps answering our seat's turns so the game can reach its end
    private void playOut(int waitSeconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(waitSeconds);
        while (!finished && !link.isClosed() && System.nanoTime() < deadline) {
            ServerMessage.YourTurn turn = ourTurn.getAndSet(null);
            if (turn != null) {
                link.request(legitMove(turn));
            } else {
                Thread.sleep(2);
            }
        }
    }

    private String finalHashVerdict() throws Exception {
        if (!finished || !sawUpdate) {
            return "n/a (game had not finished)";
        }
        ServerMessage.RoomSnapshot before = lastSnapshot.get();
        expectOk(new ClientMessage.RequestSnapshot(UUID.randomUUID(), roomId));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SETUP_TIMEOUT_SECONDS);
        while (lastSnapshot.get() == before && System.nanoTime() < deadline) {
            Thread.sleep(2);
        }
        ServerMessage.RoomSnapshot snapshot = lastSnapshot.get();
        boolean match = snapshot != null && snapshot != before && snapshot.stateHash() == lastUpdateHash;
        return String.format("client=%08X server=%s  %s", lastUpdateHash,
                snapshot == null ? "?" : String.format("%08X", snapshot.stateHash()), match ? "MATCH" : "MISMATCH");
    }

    private StressReport report(double seconds, String finalHash) {
        return new StressReport(options.name(), options.requests(), stats.responseCount(), stats.timeoutCount(),
                seconds, stats.outcomeCounts(), stats.unexpectedCount(), stats.unexpectedSamples(),
                stats.percentileMs(50), stats.percentileMs(95), stats.maxMs(), stats.chatSentCount(),
                stats.chatAcceptedCount(), stats.chatRateLimitedCount(), chatOrderHash.get(), chatLines.get(),
                updatesSeen.get(), hashMismatches.get(), finalHash);
    }

    /** Tiny helper so the sender loop reads cleanly. */
    private static final class LockSupportSleep {
        private LockSupportSleep() {
        }

        static void nanos(long nanos) {
            java.util.concurrent.locks.LockSupport.parkNanos(nanos);
        }
    }
}
