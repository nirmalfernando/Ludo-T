package com.ludot.server.app;

import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.Variant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** All live rooms, by id. Creation and lookup are safe from any connection thread. */
public final class RoomRegistry {

    private final Map<String, GameSession> rooms = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();
    private final SessionConfig config;
    private final ScheduledExecutorService scheduler;
    private final LobbyService lobby;
    private final GameRepository repository;
    private final LongSupplier clock;

    // what each room reports to: the registry forgets a closed room, then the lobby is told
    private final LobbyPublisher roomEvents = new LobbyPublisher() {
        @Override
        public void roomChanged(RoomSummary summary) {
            lobby.roomChanged(summary);
        }

        @Override
        public void roomClosed(String roomId) {
            rooms.remove(roomId);
            lobby.roomClosed(roomId);
        }
    };

    public RoomRegistry(SessionConfig config, ScheduledExecutorService scheduler, LobbyService lobby,
                        GameRepository repository, LongSupplier clock) {
        this.config = config;
        this.scheduler = scheduler;
        this.lobby = lobby;
        this.repository = repository;
        this.clock = clock;
    }

    /** Creates a room with the next id (R-0001, R-0002, ...) and lists it in the lobby. */
    public GameSession create(RoomMode mode, Variant variant, Long seed) {
        String id = String.format("R-%04d", counter.incrementAndGet());
        GameSession session = new GameSession(id, mode, variant, seed, config, scheduler, roomEvents,
                repository, clock);
        rooms.put(id, session);
        lobby.roomChanged(session.summary());
        return session;
    }

    /** The room, or null if there is no such room (or it has closed). */
    public GameSession find(String roomId) {
        return rooms.get(roomId);
    }

    public Collection<GameSession> all() {
        return rooms.values();
    }

    public int size() {
        return rooms.size();
    }

    public void shutdown() {
        rooms.values().forEach(GameSession::shutdown);
        rooms.clear();
    }
}
