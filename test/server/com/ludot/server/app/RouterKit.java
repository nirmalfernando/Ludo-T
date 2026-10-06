package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/** The lobby, registry and router wired together without any sockets, plus helpers to play fake clients. */
final class RouterKit implements AutoCloseable {

    /** A fake connected client. */
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
    final PlayerDirectory directory = new PlayerDirectory();
    final RequestRouter router;

    RouterKit() {
        this(SessionConfig.defaults().withDelays(0, 1, 1));
    }

    RouterKit(SessionConfig config) {
        this.config = config;
        this.lobby = new LobbyService(config, clock::get, repository);
        this.registry = new RoomRegistry(config, scheduler, lobby, repository, clock::get);
        this.router = new RequestRouter(directory, registry, lobby, repository);
    }

    Client connect() {
        return new Client(router, new RecordingSink());
    }

    Client connect(RecordingSink sink) {
        return new Client(router, sink);
    }

    /** A client that has already said Hello. */
    Client hello(String nickname) {
        Client client = connect();
        Response response = client.call(new ClientMessage.Hello(UUID.randomUUID(), nickname));
        if (!response.isOk()) {
            throw new AssertionError("Hello failed: " + response);
        }
        return client;
    }

    /** True if a brand new connection can take the nickname (so it was free). */
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
