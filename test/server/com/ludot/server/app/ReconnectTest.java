package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.ServerMessage.Welcome;
import com.ludot.protocol.Variant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Reconnecting after a dropped connection")
class ReconnectTest {

    private RouterKit kit;

    @AfterEach
    void close() {
        if (kit != null) {
            kit.close();
        }
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    private static Welcome welcome(RouterKit.Client client) {
        return client.inbox.await(Welcome.class, 5000);
    }

    private static Response reconnect(RouterKit.Client client, String token) {
        return client.call(new ClientMessage.Reconnect(id(), token));
    }

    @Test @DisplayName("within the grace period the same person gets their identity back and the nickname stays reserved meanwhile")
    void reconnectWithinGrace() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 10_000);
        RouterKit.Client first = kit.helloReconnectable("alice");
        Welcome original = welcome(first);

        kit.router.disconnected(first.session);
        assertEquals(1, kit.directory.detachedCount());
        assertFalse(kit.connectAndHello("alice"), "the nickname must stay reserved while alice may come back");

        RouterKit.Client second = kit.connect();
        assertTrue(reconnect(second, original.token()).isOk());
        Welcome again = welcome(second);
        assertEquals(original.playerId(), again.playerId());
        assertEquals("alice", again.nickname());
        assertEquals(original.token(), again.token());
        assertEquals(0, kit.directory.detachedCount());
        assertEquals(1, kit.directory.connectedCount());
    }

    @Test @DisplayName("after the grace period the nickname is free again and the token no longer works")
    void expires() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 150);
        RouterKit.Client first = kit.helloReconnectable("alice");
        Welcome original = welcome(first);
        kit.router.disconnected(first.session);

        Kit.await(() -> kit.directory.detachedCount() == 0, 5000);
        assertEquals(0, kit.directory.detachedCount());
        Response late = reconnect(kit.connect(), original.token());
        assertEquals(ErrorCode.UNKNOWN_TOKEN, late.errorCode());
        assertTrue(kit.connectAndHello("alice"), "the nickname should be free after the grace period");
    }

    @Test @DisplayName("a token nobody issued is refused with UNKNOWN_TOKEN")
    void unknownToken() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 10_000);
        assertEquals(ErrorCode.UNKNOWN_TOKEN, reconnect(kit.connect(), "not-a-token").errorCode());
    }

    @Test @DisplayName("a client that did not ask to be reconnectable (the bots) loses its identity at once")
    void notReconnectableByDefault() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 10_000);
        RouterKit.Client bot = kit.hello("bot_one");
        Welcome original = welcome(bot);
        kit.router.disconnected(bot.session);

        assertEquals(0, kit.directory.detachedCount());
        assertEquals(ErrorCode.UNKNOWN_TOKEN, reconnect(kit.connect(), original.token()).errorCode());
        assertTrue(kit.connectAndHello("bot_one"));
    }

    @Test @DisplayName("with the grace period switched off nothing is held, even for a reconnectable client")
    void graceDisabled() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 0);
        RouterKit.Client first = kit.helloReconnectable("alice");
        Welcome original = welcome(first);
        kit.router.disconnected(first.session);
        assertEquals(ErrorCode.UNKNOWN_TOKEN, reconnect(kit.connect(), original.token()).errorCode());
        assertTrue(kit.connectAndHello("alice"));
    }

    @Test @DisplayName("SignOut frees the nickname at once and the token stops working")
    void signOutFreesAtOnce() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 10_000);
        RouterKit.Client alice = kit.helloReconnectable("alice");
        Welcome original = welcome(alice);
        assertTrue(alice.call(new ClientMessage.SignOut(id())).isOk());

        assertTrue(kit.connectAndHello("alice"), "the nickname should be free straight after SignOut");
        assertEquals(ErrorCode.UNKNOWN_TOKEN, reconnect(kit.connect(), original.token()).errorCode());
        kit.router.disconnected(alice.session); // the socket closing afterwards must not disturb the new owner
        assertEquals(1, kit.directory.connectedCount());
    }

    @Test @DisplayName("reconnecting while the old connection still looks alive takes it over and closes it")
    void takesOverHalfOpenConnection() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 10_000);
        RouterKit.Client old = kit.helloReconnectable("alice");
        Welcome original = welcome(old);

        RouterKit.Client fresh = kit.connect();
        assertTrue(reconnect(fresh, original.token()).isOk());
        assertTrue(old.inbox.isClosed(), "the old connection should have been closed");

        kit.router.disconnected(old.session); // its clean-up runs late and must not detach the new connection
        assertEquals(0, kit.directory.detachedCount());
        assertEquals(1, kit.directory.connectedCount());
        assertFalse(kit.connectAndHello("alice"));
    }

    @Test @DisplayName("a second Reconnect on an already identified connection is a protocol error")
    void alreadyIdentified() {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 10_000);
        RouterKit.Client alice = kit.helloReconnectable("alice");
        Welcome original = welcome(alice);
        assertEquals(ErrorCode.PROTOCOL_ERROR, reconnect(alice, original.token()).errorCode());
    }

    @Test @DisplayName("two connections racing to reconnect with one token: exactly one stays open")
    void racingReconnects() throws Exception {
        for (int round = 0; round < 20; round++) {
            kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1), 10_000);
            RouterKit.Client first = kit.helloReconnectable("alice");
            String token = welcome(first).token();
            kit.router.disconnected(first.session);

            RouterKit.Client a = kit.connect();
            RouterKit.Client b = kit.connect();
            CountDownLatch go = new CountDownLatch(1);
            Thread ta = new Thread(() -> {
                await(go);
                a.send(new ClientMessage.Reconnect(id(), token));
            });
            Thread tb = new Thread(() -> {
                await(go);
                b.send(new ClientMessage.Reconnect(id(), token));
            });
            ta.start();
            tb.start();
            go.countDown();
            ta.join();
            tb.join();
            Kit.await(() -> a.inbox.isClosed() != b.inbox.isClosed(), 5000);

            assertTrue(a.inbox.isClosed() ^ b.inbox.isClosed(), "exactly one of the two connections must have been closed");
            assertEquals(1, kit.directory.connectedCount());
            kit.close();
        }
        kit = null;
    }

    @Test @DisplayName("rejoining the room gives a player back the seat they held when the connection dropped, and says so")
    void seatIsRestoredOnRejoin() throws Exception {
        kit = new RouterKit(SessionConfig.defaults(), 10_000); // normal AI pace: the game cannot finish while we test
        RouterKit.Client alice = kit.helloReconnectable("alice");
        Welcome original = welcome(alice);
        String roomId = (String) alice.call(new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L)).payload();
        RouterKit.Client observer = kit.hello("observer");
        assertTrue(observer.call(new ClientMessage.JoinRoom(id(), roomId)).isOk());
        assertTrue(alice.call(new ClientMessage.ClaimSeat(id(), roomId, Colour.RED)).isOk());
        assertTrue(alice.call(new ClientMessage.StartGame(id(), roomId)).isOk());

        kit.router.disconnected(alice.session);
        assertEquals(Controller.DISCONNECTED, redSeat(latestSnapshot(observer, roomId)).controller());

        RouterKit.Client back = kit.connect();
        assertTrue(reconnect(back, original.token()).isOk());
        assertTrue(back.call(new ClientMessage.JoinRoom(id(), roomId)).isOk());
        ServerMessage.RoomSnapshot snapshot = back.inbox.await(ServerMessage.RoomSnapshot.class, 5000);

        assertEquals(Controller.HUMAN, redSeat(snapshot).controller());
        assertEquals(original.playerId(), redSeat(snapshot).playerId());
        assertEquals(Controller.HUMAN, redSeat(latestSnapshot(observer, roomId)).controller());
        assertTrue(observer.inbox.of(ServerMessage.ChatMessage.class).stream()
                        .anyMatch(m -> m.system() && m.text().equals("alice is back")),
                "the room should announce the return");
    }

    @Test @DisplayName("someone who never held a seat rejoins as a spectator, as before")
    void spectatorRejoinsAsSpectator() throws Exception {
        kit = new RouterKit(SessionConfig.defaults(), 10_000);
        RouterKit.Client host = kit.hello("host_h");
        String roomId = (String) host.call(new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L)).payload();
        RouterKit.Client watcher = kit.helloReconnectable("watcher_w");
        Welcome original = welcome(watcher);
        assertTrue(watcher.call(new ClientMessage.JoinRoom(id(), roomId)).isOk());
        kit.router.disconnected(watcher.session);

        RouterKit.Client back = kit.connect();
        assertTrue(reconnect(back, original.token()).isOk());
        assertTrue(back.call(new ClientMessage.JoinRoom(id(), roomId)).isOk());
        kit.registry.find(roomId).awaitIdle();
        assertTrue(host.inbox.of(ServerMessage.ChatMessage.class).stream()
                .anyMatch(m -> m.system() && m.text().equals("watcher_w joined as a spectator")));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static com.ludot.protocol.SeatDto redSeat(ServerMessage.RoomSnapshot snapshot) {
        return snapshot.seats().stream().filter(seat -> seat.colour() == Colour.RED).findFirst().orElseThrow();
    }

    // asks for a snapshot; the response comes before the snapshot itself, so a second request makes sure it has arrived
    private static ServerMessage.RoomSnapshot latestSnapshot(RouterKit.Client client, String roomId) {
        client.call(new ClientMessage.RequestSnapshot(id(), roomId));
        client.call(new ClientMessage.RequestSnapshot(id(), roomId));
        List<ServerMessage.RoomSnapshot> all = client.inbox.of(ServerMessage.RoomSnapshot.class);
        return all.get(all.size() - 1);
    }
}
