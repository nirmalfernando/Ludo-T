package com.ludot.server;

import com.ludot.server.adapters.net.ConnectionAcceptor;
import com.ludot.server.app.GameRepository;
import com.ludot.server.app.LobbyService;
import com.ludot.server.app.PlayerDirectory;
import com.ludot.server.app.RequestRouter;
import com.ludot.server.app.RoomRegistry;
import com.ludot.server.concurrent.NamedThreadFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Composition root: builds the lobby, the room registry and the router, then listens for connections.
 * It is the only class that knows every concrete piece; everything else depends on small interfaces.
 */
public final class GameServer implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(GameServer.class.getName());

    private final ServerConfig config;
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2,
            new NamedThreadFactory("scheduler", true));
    private final LobbyService lobby;
    private final RoomRegistry registry;
    private final ConnectionAcceptor acceptor;
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final Map<String, Integer> lastQueueDepth = new HashMap<>();

    public GameServer(ServerConfig config, GameRepository repository) {
        this(config, repository, 0);
    }

    /** @param lastRoomNumber the highest room number already stored, so new rooms never reuse an id */
    public GameServer(ServerConfig config, GameRepository repository, long lastRoomNumber) {
        this.config = config;
        scheduler.setRemoveOnCancelPolicy(true);
        this.lobby = new LobbyService(config.session(), System::currentTimeMillis, repository);
        this.registry = new RoomRegistry(config.session(), scheduler, lobby, repository, System::currentTimeMillis,
                lastRoomNumber);
        RequestRouter router = new RequestRouter(new PlayerDirectory(), registry, lobby, repository);
        this.acceptor = new ConnectionAcceptor(config.port(), router, config.outboundCapacity(), config.readTimeoutMs(),
                config.maxInFlight());
    }

    /** Binds the port and starts accepting connections. */
    public void start() throws IOException {
        acceptor.start();
        scheduler.scheduleAtFixedRate(this::logQueueDepths, config.queueLogIntervalMs(),
                config.queueLogIntervalMs(), TimeUnit.MILLISECONDS);
        LOG.info("LUDO-T server listening on port " + port());
    }

    /** The port actually bound (useful when the configured port is 0). */
    public int port() {
        return acceptor.port();
    }

    public RoomRegistry registry() {
        return registry;
    }

    public LobbyService lobby() {
        return lobby;
    }

    /** Blocks until the server is closed. */
    public void awaitClose() throws InterruptedException {
        stopped.await();
    }

    // only the scheduler thread touches lastQueueDepth; logs while a room is backed up and once when it drains
    private void logQueueDepths() {
        registry.all().forEach(room -> {
            int depth = room.queueDepth();
            int previous = lastQueueDepth.getOrDefault(room.roomId(), 0);
            if (depth > 0 || previous > 0) {
                LOG.info("room " + room.roomId() + " queueDepth=" + depth);
            }
            lastQueueDepth.put(room.roomId(), depth);
        });
    }

    @Override
    public void close() {
        acceptor.close();
        registry.shutdown();
        lobby.close();
        scheduler.shutdownNow();
        stopped.countDown();
    }
}
