package com.ludot.integration;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerLink;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.ServerMessage.RoomSnapshot;
import com.ludot.protocol.ServerMessage.Welcome;
import com.ludot.protocol.Variant;
import com.ludot.server.GameServer;
import com.ludot.server.ServerConfig;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import com.ludot.server.app.SessionConfig;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A real connection is cut and a new one takes over the same person, all over TCP. */
@DisplayName("Reconnect over real sockets")
class ReconnectOverTcpTest {

    private GameServer server;

    @BeforeEach void startServer() throws Exception {
        // the normal AI pace, so the game cannot finish while the test is running
        server = new GameServer(ServerConfig.defaults().withPort(0).withReconnectGraceMs(10_000)
                .withSession(SessionConfig.defaults()), new InMemoryGameRepository());
        server.start();
    }

    @AfterEach void stopServer() {
        server.close();
    }

    private static Response ask(ServerLink link, ClientMessage.Request request) throws Exception {
        return link.request(request).get(5, TimeUnit.SECONDS);
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    /** A connection that records everything the server pushes to it. */
    private static final class Listening {
        final ServerLink link;
        final List<ServerMessage> inbox = new CopyOnWriteArrayList<>();

        Listening(ServerLink link) {
            this.link = link;
            link.addListener(inbox::add);
        }

        <T extends ServerMessage> T await(Class<T> type, java.util.function.Predicate<T> match) throws Exception {
            long deadline = System.currentTimeMillis() + 5_000;
            while (System.currentTimeMillis() < deadline) {
                for (ServerMessage message : inbox) {
                    if (type.isInstance(message) && match.test(type.cast(message))) {
                        return type.cast(message);
                    }
                }
                Thread.sleep(10);
            }
            throw new AssertionError("Nothing of type " + type.getSimpleName() + " arrived in time");
        }
    }

    @Test @DisplayName("a player whose connection is cut gets the same identity and seat back on a new connection")
    void cutAndComeBack() throws Exception {
        Listening alice = new Listening(ServerLink.connect("localhost", server.port()));
        assertTrue(ask(alice.link, new ClientMessage.Hello(id(), "alice", true)).isOk());
        Welcome welcome = alice.await(Welcome.class, w -> true);

        String roomId = (String) ask(alice.link, new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L)).payload();
        Listening observer = new Listening(ServerLink.connect("localhost", server.port()));
        assertTrue(ask(observer.link, new ClientMessage.Hello(id(), "observer")).isOk());
        assertTrue(ask(observer.link, new ClientMessage.JoinRoom(id(), roomId)).isOk());
        assertTrue(ask(alice.link, new ClientMessage.ClaimSeat(id(), roomId, Colour.BLUE)).isOk());
        assertTrue(ask(alice.link, new ClientMessage.StartGame(id(), roomId)).isOk());

        alice.link.close(); // the socket is cut: no goodbye

        // the room notices and hands alice's seat to the AI
        observer.await(ServerMessage.SeatsUpdated.class, update -> update.seats().stream()
                .anyMatch(seat -> seat.colour() == Colour.BLUE && seat.controller() == Controller.DISCONNECTED));

        Listening back = new Listening(ServerLink.connect("localhost", server.port()));
        assertTrue(ask(back.link, new ClientMessage.Reconnect(id(), welcome.token())).isOk());
        Welcome again = back.await(Welcome.class, w -> true);
        assertEquals(welcome.playerId(), again.playerId());
        assertEquals("alice", again.nickname());

        assertTrue(ask(back.link, new ClientMessage.JoinRoom(id(), roomId)).isOk());
        RoomSnapshot snapshot = back.await(RoomSnapshot.class, s -> true);
        var blue = snapshot.seats().stream().filter(seat -> seat.colour() == Colour.BLUE).findFirst().orElseThrow();
        assertEquals(Controller.HUMAN, blue.controller());
        assertEquals(welcome.playerId(), blue.playerId());

        // and the others see her return
        observer.await(ServerMessage.ChatMessage.class, chat -> chat.system() && chat.text().equals("alice is back"));
        observer.link.close();
        back.link.close();
    }

    @Test @DisplayName("a token cannot be used once the person signed out politely")
    void signOutEndsTheToken() throws Exception {
        Listening alice = new Listening(ServerLink.connect("localhost", server.port()));
        assertTrue(ask(alice.link, new ClientMessage.Hello(id(), "alice", true)).isOk());
        Welcome welcome = alice.await(Welcome.class, w -> true);
        assertTrue(ask(alice.link, new ClientMessage.SignOut(id())).isOk());
        alice.link.close();

        try (ServerLink fresh = ServerLink.connect("localhost", server.port())) {
            Response refused = ask(fresh, new ClientMessage.Reconnect(id(), welcome.token()));
            assertFalse(refused.isOk());
            assertEquals(ErrorCode.UNKNOWN_TOKEN, refused.errorCode());
        }
        try (ServerLink again = ServerLink.connect("localhost", server.port())) {
            assertTrue(ask(again, new ClientMessage.Hello(id(), "alice", true)).isOk(), "the nickname should be free");
        }
    }

    @Test @DisplayName("while the grace period runs, another person cannot take the nickname")
    void nicknameStaysReserved() throws Exception {
        Listening alice = new Listening(ServerLink.connect("localhost", server.port()));
        assertTrue(ask(alice.link, new ClientMessage.Hello(id(), "alice", true)).isOk());
        alice.link.close();
        Thread.sleep(300); // let the server notice the cut

        try (ServerLink thief = ServerLink.connect("localhost", server.port())) {
            Response refused = ask(thief, new ClientMessage.Hello(id(), "alice", true));
            assertFalse(refused.isOk());
            assertEquals(ErrorCode.NICKNAME_IN_USE, refused.errorCode());
        }
    }
}
