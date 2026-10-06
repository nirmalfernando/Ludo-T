package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Variant;
import com.ludot.server.GameServer;
import com.ludot.server.ServerConfig;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The whole server over real TCP sockets on a random port. */
@DisplayName("GameServer over TCP")
class GameServerTest {

    private GameServer server;
    private final List<TestClient> clients = new ArrayList<>();
    private InMemoryGameRepository repository;

    @BeforeEach void startServer() throws IOException {
        start(ServerConfig.defaults().withPort(0)
                .withSession(SessionConfig.defaults().withDelays(0, 1, 1)));
    }

    private void start(ServerConfig config) throws IOException {
        if (server != null) {
            server.close();
        }
        repository = new InMemoryGameRepository();
        server = new GameServer(config, repository);
        server.start();
    }

    @AfterEach void stopServer() {
        clients.forEach(TestClient::close);
        server.close();
    }

    private TestClient client(String nickname) throws IOException {
        TestClient client = TestClient.connect(server.port());
        clients.add(client);
        return nickname == null ? client : client.hello(nickname);
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    private static String createBotMatch(TestClient host, long seed) throws IOException {
        return (String) host.call(new ClientMessage.CreateRoom(id(), RoomMode.BOT_MATCH, Variant.LUDO_T, seed)).payload();
    }

    private static String createPlayRoom(TestClient host, long seed) throws IOException {
        return (String) host.call(new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, seed)).payload();
    }

    @Test @DisplayName("Hello over a socket gives Welcome then OK; Ping gives Pong")
    void handshake() throws IOException {
        TestClient alice = client(null);
        Response hello = alice.call(new ClientMessage.Hello(id(), "alice"));
        assertTrue(hello.isOk());
        assertInstanceOf(ServerMessage.Welcome.class, alice.inbox.messages().get(0));
        alice.send(new ClientMessage.Ping());
        assertNotNull(alice.inbox.await(ServerMessage.Pong.class, 2000));
    }

    @Test @DisplayName("a duplicate nickname is refused over the wire")
    void duplicateNickname() throws IOException {
        client("alice");
        TestClient other = client(null);
        assertEquals(ErrorCode.NICKNAME_IN_USE,
                other.call(new ClientMessage.Hello(id(), "alice")).errorCode());
    }

    @Test @DisplayName("a request before Hello gets PROTOCOL_ERROR and the connection is closed")
    void requestBeforeHelloClosesConnection() throws IOException {
        TestClient rude = client(null);
        Response response = rude.call(new ClientMessage.ListRooms(id()));
        assertEquals(ErrorCode.PROTOCOL_ERROR, response.errorCode());
        assertTrue(rude.awaitDisconnected(2000));
    }

    @Test @DisplayName("garbage bytes close that connection only; the server keeps serving others")
    void garbageClosesOnlyThatConnection() throws IOException {
        TestClient good = client("good");
        TestClient bad = client(null);
        bad.sendRaw(new byte[]{0, 0, 0, 8, 1, 2, 3, 4, 5, 6, 7, 8});
        assertTrue(bad.awaitDisconnected(2000));
        assertTrue(good.call(new ClientMessage.ListRooms(id())).isOk());
    }

    @Test @DisplayName("an absurd frame length closes the connection")
    void oversizedFrameClosesConnection() throws IOException {
        TestClient bad = client(null);
        bad.sendRaw(new byte[]{0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff});
        assertTrue(bad.awaitDisconnected(2000));
    }

    @Test @DisplayName("a client that goes silent is dropped after the read timeout, freeing its nickname, while a pinging one stays")
    void heartbeatTimeout() throws Exception {
        start(ServerConfig.defaults().withPort(0).withReadTimeoutMs(300));
        TestClient silent = client("silent");
        TestClient alive = client("alive");

        long until = System.currentTimeMillis() + 800;
        while (System.currentTimeMillis() < until) {
            alive.send(new ClientMessage.Ping());
            Kit.sleep(100);
        }
        assertTrue(silent.awaitDisconnected(2000));
        assertFalse(alive.awaitDisconnected(10));
        assertTrue(client(null).call(new ClientMessage.Hello(id(), "silent")).isOk(), "nickname should be free again");
    }

    @Test @DisplayName("an abrupt disconnect hands the player's seat to the AI, visible to the others")
    void abruptDisconnect() throws IOException {
        TestClient alice = client("alice");
        TestClient bob = client("bob");
        String roomId = createPlayRoom(alice, 1);
        bob.call(new ClientMessage.JoinRoom(id(), roomId));
        alice.call(new ClientMessage.ClaimSeat(id(), roomId, Colour.RED));
        alice.call(new ClientMessage.StartGame(id(), roomId));
        bob.inbox.await(ServerMessage.StateUpdate.class, 5000);

        alice.close();
        bob.inbox.await(ServerMessage.SeatsUpdated.class, s -> s.seats().stream().anyMatch(x ->
                x.colour() == Colour.RED && x.controller() == Controller.DISCONNECTED), 5000);
    }

    @Test @DisplayName("three clients on three connections watch a bot match and finish with identical state")
    void spectatorsAgree() throws IOException {
        TestClient host = client("host");
        TestClient s1 = client("spec1");
        TestClient s2 = client("spec2");
        String roomId = createBotMatch(host, 1);
        s1.call(new ClientMessage.JoinRoom(id(), roomId));
        s2.call(new ClientMessage.JoinRoom(id(), roomId));

        for (TestClient c : List.of(host, s1, s2)) {
            c.inbox.await(ServerMessage.GameOver.class, 60_000);
        }
        long hash = lastHash(host);
        assertEquals(hash, lastHash(s1));
        assertEquals(hash, lastHash(s2));
    }

    private static long lastHash(TestClient client) {
        return client.inbox.of(ServerMessage.StateUpdate.class).stream().reduce((a, b) -> b).get().stateHash();
    }

    @Test @DisplayName("four people on four connections claim the four colours and play a game to the end")
    void fourPlayersOverTcp() throws IOException {
        TestClient host = client("player0");
        String roomId = createPlayRoom(host, 1);
        List<TestClient> players = new ArrayList<>(List.of(host));
        for (int i = 1; i < 4; i++) {
            TestClient player = client("player" + i);
            player.call(new ClientMessage.JoinRoom(id(), roomId));
            players.add(player);
        }
        Colour[] colours = Colour.values();
        for (int i = 0; i < 4; i++) {
            assertTrue(players.get(i).call(new ClientMessage.ClaimSeat(id(), roomId, colours[i])).isOk());
        }
        assertTrue(host.call(new ClientMessage.StartGame(id(), roomId)).isOk());

        List<Thread> threads = new ArrayList<>();
        List<Boolean> finished = java.util.Collections.synchronizedList(new ArrayList<>());
        for (TestClient player : players) {
            Thread thread = new Thread(() -> {
                try {
                    finished.add(player.playUntilGameOver(roomId, 120_000));
                } catch (IOException e) {
                    finished.add(false);
                }
            });
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join(130_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        assertEquals(4, finished.size());
        assertTrue(finished.stream().allMatch(Boolean::booleanValue), "every player should see GameOver");
    }

    @Test @DisplayName("two clients flooding 2,000 pipelined requests each all get an answer, none of them unexpected")
    void asyncFlood() throws IOException {
        start(ServerConfig.defaults().withPort(0)
                .withSession(SessionConfig.defaults().withDelays(5, 5, 5)));
        TestClient a = client("floodA");
        TestClient b = client("floodB");
        String roomId = createBotMatch(a, 1);
        b.call(new ClientMessage.JoinRoom(id(), roomId));

        List<UUID> idsA = new ArrayList<>();
        List<UUID> idsB = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            ClientMessage.Roll fromA = new ClientMessage.Roll(id(), roomId, i);
            ClientMessage.Roll fromB = new ClientMessage.Roll(id(), roomId, i);
            idsA.add(fromA.requestId());
            idsB.add(fromB.requestId());
            a.send(fromA);
            b.send(fromB);
        }
        for (UUID requestId : idsA) {
            assertExpectedRejection(a.inbox.responseTo(requestId));
        }
        for (UUID requestId : idsB) {
            assertExpectedRejection(b.inbox.responseTo(requestId));
        }
    }

    // spectators in a bot match can never roll: NOT_A_PLAYER, or a state error if the game ended or paused meanwhile
    private static void assertExpectedRejection(Response response) {
        assertFalse(response.isOk());
        assertTrue(List.of(ErrorCode.NOT_A_PLAYER, ErrorCode.GAME_FINISHED, ErrorCode.GAME_PAUSED)
                .contains(response.errorCode()), String.valueOf(response));
    }

    @Test @DisplayName("a client pipelining 3,000 snapshot requests into a tiny outbound queue is slowed, not dropped")
    void inFlightWindowProtectsAFastClient() throws IOException {
        start(ServerConfig.defaults().withPort(0).withOutboundCapacity(40).withMaxInFlight(16)
                .withSession(SessionConfig.defaults().withDelays(0, 1, 1)));
        TestClient flooder = client("flooder");
        String roomId = createPlayRoom(flooder, 1);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            ClientMessage.RequestSnapshot request = new ClientMessage.RequestSnapshot(id(), roomId);
            ids.add(request.requestId());
            flooder.send(request);
        }
        for (UUID requestId : ids) {
            assertTrue(flooder.inbox.responseTo(requestId).isOk());
        }
        assertFalse(flooder.awaitDisconnected(50), "the server must not drop a client it can slow down instead");
    }

    @Test @DisplayName("lobby updates reach a connected client when someone else creates a room")
    void lobbyIsLiveOverTcp() throws IOException {
        TestClient watcher = client("watcher");
        watcher.call(new ClientMessage.ListRooms(id()));
        TestClient maker = client("maker");
        String roomId = createPlayRoom(maker, 1);
        watcher.inbox.await(ServerMessage.LobbyUpdate.class,
                u -> u.rooms().stream().anyMatch(r -> r.roomId().equals(roomId)), 5000);
    }
}
