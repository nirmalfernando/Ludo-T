package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Variant;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/** Test fixture: a scheduler, a controllable clock, a recording lobby and an in-memory repository. */
final class Kit implements AutoCloseable {

    static final class RecordingLobby implements LobbyPublisher {
        final List<RoomSummary> changes = new CopyOnWriteArrayList<>();
        final List<String> closed = new CopyOnWriteArrayList<>();

        @Override
        public void roomChanged(RoomSummary summary) {
            changes.add(summary);
        }

        @Override
        public void roomClosed(String roomId) {
            closed.add(roomId);
        }
    }

    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r, "test-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    final InMemoryGameRepository repository = new InMemoryGameRepository();
    final RecordingLobby lobby = new RecordingLobby();
    final AtomicLong clock = new AtomicLong(1_000_000);
    SessionConfig config = SessionConfig.defaults().withDelays(0, 1, 1);
    private final AtomicInteger roomCounter = new AtomicInteger();
    private final List<GameSession> sessions = new CopyOnWriteArrayList<>();

    GameSession room(RoomMode mode, long seed) {
        GameSession session = new GameSession("R-" + roomCounter.incrementAndGet(), mode, Variant.LUDO_T, seed,
                config, scheduler, lobby, repository, clock::get);
        sessions.add(session);
        return session;
    }

    static Participant person(String name) {
        return new Participant(new PlayerIdentity("id-" + name, name, "token-" + name), new RecordingSink());
    }

    static RecordingSink sink(Participant participant) {
        return (RecordingSink) participant.sink();
    }

    /** Sends a request and waits for its response. */
    static Response call(GameSession room, Participant from, ClientMessage.Request request) {
        room.submit(from, request);
        return sink(from).responseTo(request.requestId());
    }

    /** Because the room processes commands in order, once this returns everything submitted before it has been handled. */
    static void settle(GameSession room, Participant from) {
        call(room, from, new ClientMessage.RequestSnapshot(UUID.randomUUID(), room.roomId()));
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    static void await(BooleanSupplier condition, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("Condition not met within " + timeoutMs + " ms");
    }

    /** Creates a room with the participant as host and waits for the creator's snapshot. */
    GameSession roomWith(RoomMode mode, long seed, Participant creator) {
        GameSession room = room(mode, seed);
        room.attachCreator(creator);
        sink(creator).await(ServerMessage.RoomSnapshot.class, 5000);
        return room;
    }

    static ClientMessage.JoinRoom join(GameSession room) {
        return new ClientMessage.JoinRoom(UUID.randomUUID(), room.roomId());
    }

    @Override
    public void close() {
        sessions.forEach(GameSession::shutdown);
        scheduler.shutdownNow();
    }
}
