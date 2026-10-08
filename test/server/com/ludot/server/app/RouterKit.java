package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

final class RouterKit implements AutoCloseable {

    static final class Client {
        final ClientSession session;
        final RecordingSink inbox;
        private final RequestRouter router;

        Client(RequestRouter router, RecordingSink inbox) {
            this.router = router;
            this.inbox = inbox;
            this.session = new ClientSession(inbox);
        }

        boolean send(ClientMessage message) {
            return router.route(session, message);
        }

        Response call(ClientMessage.Request request) {
            send(request);
            return inbox.responseTo(request.requestId());
        }

        Participant participant() {
            return session.participant();
        }

        String id() {
            return session.participant().id();
        }
    }

    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r, "test-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    final AtomicLong clock = new AtomicLong(1_000_000);
    final SessionConfig config;
    final LobbyService lobby;
    final RoomRegistry registry;
    final InMemoryGameRepository repository = new InMemoryGameRepository();
    final PlayerDirectory directory;
    final ReplayService replays;
    final RequestRouter router;

    RouterKit() {
        this(SessionConfig.defaults().withDelays(0, 1, 1));
    }

    RouterKit(SessionConfig config) {
        this(config, 0);
    }

    RouterKit(SessionConfig config, long reconnectGraceMs) {
        this.config = config;
        this.directory = new PlayerDirectory(scheduler, reconnectGraceMs);
        this.lobby = new LobbyService(config, clock::get, repository);
        this.registry = new RoomRegistry(config, scheduler, lobby, repository, clock::get);
        this.replays = new ReplayService(repository, scheduler, speed -> 1L);
        this.router = new RequestRouter(directory, registry, lobby, repository, repository, replays);
    }

    Client connect() {
        return new Client(router, new RecordingSink());
    }

    Client connect(RecordingSink sink) {
        return new Client(router, sink);
    }

    Client hello(String nickname) {
        Client client = connect();
        Response response = client.call(new ClientMessage.Hello(UUID.randomUUID(), nickname));
        if (!response.isOk()) {
            throw new AssertionError("Hello failed: " + response);
        }
        return client;
    }

    Client helloReconnectable(String nickname) {
        Client client = connect();
        Response response = client.call(new ClientMessage.Hello(UUID.randomUUID(), nickname, true));
        if (!response.isOk()) {
            throw new AssertionError("Hello failed: " + response);
        }
        return client;
    }

    // true if a brand new connection can take the nickname (so it was free)
    boolean connectAndHello(String nickname) {
        return connect().call(new ClientMessage.Hello(UUID.randomUUID(), nickname)).isOk();
    }

    @Override
    public void close() {
        registry.shutdown();
        lobby.close();
        scheduler.shutdownNow();
    }
}
