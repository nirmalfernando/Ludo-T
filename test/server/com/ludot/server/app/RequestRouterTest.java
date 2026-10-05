package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomList;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Variant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RequestRouter, lobby and registry")
class RequestRouterTest {

    private RouterKit kit;

    @BeforeEach void setUp() {
        kit = new RouterKit();
    }

    @AfterEach void tearDown() {
        kit.close();
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    private static ClientMessage.CreateRoom playRoom() {
        return new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L);
    }

    private static ClientMessage.CreateRoom botMatch() {
        return new ClientMessage.CreateRoom(id(), RoomMode.BOT_MATCH, Variant.LUDO_T, 1L);
    }

    // ---------------------------------------------------------------- identity

    @Test @DisplayName("Hello answers Welcome with a token, then OK")
    void helloWelcomes() {
        RouterKit.Client alice = kit.connect();
        ClientMessage.Hello hello = new ClientMessage.Hello(id(), "alice");
        assertTrue(alice.send(hello));
        Response response = alice.inbox.responseTo(hello.requestId());
        assertTrue(response.isOk());
        ServerMessage.Welcome welcome = alice.inbox.of(ServerMessage.Welcome.class).get(0);
        assertEquals("alice", welcome.nickname());
        assertFalse(welcome.token().isBlank());
        assertInstanceOf(ServerMessage.Welcome.class, alice.inbox.messages().get(0));
    }

    @Test @DisplayName("bad nicknames are refused and the connection stays open to try again")
    void invalidNickname() {
        RouterKit.Client client = kit.connect();
        for (String bad : List.of("ab", "has space", "waytoolongnicknamehere", "semi;colon")) {
            Response response = client.call(new ClientMessage.Hello(id(), bad));
            assertEquals(ErrorCode.INVALID_NICKNAME, response.errorCode(), bad);
        }
        assertTrue(client.call(new ClientMessage.Hello(id(), "fine_name")).isOk());
    }

    @Test @DisplayName("a nickname is unique among connected people, ignoring case, and is freed on disconnect")
    void nicknameUniqueness() {
        RouterKit.Client first = kit.hello("Alice");
        RouterKit.Client second = kit.connect();
        assertEquals(ErrorCode.NICKNAME_IN_USE,
                second.call(new ClientMessage.Hello(id(), "alice")).errorCode());

        kit.router.disconnected(first.session);
        assertTrue(second.call(new ClientMessage.Hello(id(), "alice")).isOk());
    }

    @Test @DisplayName("anything but Hello or Ping before identifying is a protocol error that closes the connection")
    void requestBeforeHello() {
        RouterKit.Client client = kit.connect();
        ClientMessage.ListRooms early = new ClientMessage.ListRooms(id());
        assertFalse(client.send(early));
        assertEquals(ErrorCode.PROTOCOL_ERROR, client.inbox.responseTo(early.requestId()).errorCode());
    }

    @Test @DisplayName("Ping is answered with Pong, even before Hello")
    void pingPong() {
        RouterKit.Client client = kit.connect();
        assertTrue(client.send(new ClientMessage.Ping()));
        assertNotNull(client.inbox.await(ServerMessage.Pong.class, 1000));
    }

    @Test @DisplayName("a second Hello on the same connection is refused")
    void helloTwice() {
        RouterKit.Client client = kit.hello("alice");
        assertEquals(ErrorCode.PROTOCOL_ERROR,
                client.call(new ClientMessage.Hello(id(), "another")).errorCode());
    }

    // ---------------------------------------------------------------- rooms and lobby

    @Test @DisplayName("creating a room answers with its id, then the snapshot, and lists it in the lobby")
    void createRoom() throws InterruptedException {
        RouterKit.Client alice = kit.hello("alice");
        RouterKit.Client bob = kit.hello("bob");
        Response listed = bob.call(new ClientMessage.ListRooms(id()));
        assertTrue(((RoomList) listed.payload()).rooms().isEmpty());

        ClientMessage.CreateRoom create = playRoom();
        Response created = alice.call(create);
        assertTrue(created.isOk());
        String roomId = (String) created.payload();
        assertEquals("R-0001", roomId);

        alice.inbox.await(ServerMessage.RoomSnapshot.class, 5000);
        List<ServerMessage> seen = alice.inbox.messages();
        int responseAt = seen.indexOf(created);
        int snapshotAt = seen.indexOf(alice.inbox.of(ServerMessage.RoomSnapshot.class).get(0));
        assertTrue(responseAt < snapshotAt, "response must precede the snapshot");

        ServerMessage.LobbyUpdate update = bob.inbox.await(ServerMessage.LobbyUpdate.class,
                u -> u.rooms().stream().anyMatch(r -> r.roomId().equals(roomId) && r.hostNickname().equals("alice")), 5000);
        assertEquals(RoomMode.PLAY, update.rooms().get(0).mode());
    }

    @Test @DisplayName("creating the same request twice makes only one room")
    void duplicateCreateRequest() {
        RouterKit.Client alice = kit.hello("alice");
        ClientMessage.CreateRoom create = playRoom();
        Response first = alice.call(create);
        alice.send(create);
        assertEquals(1, kit.registry.size());
        assertEquals(2, alice.inbox.of(Response.class).stream().filter(r -> r.requestId().equals(create.requestId())).count());
        assertEquals(first, alice.inbox.of(Response.class).stream()
                .filter(r -> r.requestId().equals(create.requestId())).skip(1).findFirst().get());
    }

    @Test @DisplayName("joining and acting in a room that does not exist is UNKNOWN_ROOM")
    void unknownRoom() {
        RouterKit.Client alice = kit.hello("alice");
        assertEquals(ErrorCode.UNKNOWN_ROOM, alice.call(new ClientMessage.JoinRoom(id(), "R-9999")).errorCode());
        assertEquals(ErrorCode.UNKNOWN_ROOM, alice.call(new ClientMessage.Roll(id(), "R-9999", 0)).errorCode());
    }

    @Test @DisplayName("two clients in one room: a join reaches the room and the second client can play in it")
    void joinThroughRouter() {
        RouterKit.Client alice = kit.hello("alice");
        RouterKit.Client bob = kit.hello("bob");
        String roomId = (String) alice.call(playRoom()).payload();
        assertTrue(bob.call(new ClientMessage.JoinRoom(id(), roomId)).isOk());
        assertTrue(bob.call(new ClientMessage.ClaimSeat(id(), roomId, com.ludot.protocol.Colour.BLUE)).isOk());
        alice.inbox.await(ServerMessage.SeatsUpdated.class,
                s -> s.seats().stream().anyMatch(x -> "bob".equals(x.nickname())), 5000);
    }

    @Test @DisplayName("a late lobby subscriber gets the current rooms and recent chat in the ListRooms answer")
    void lobbyListIncludesChat() throws InterruptedException {
        RouterKit.Client alice = kit.hello("alice");
        alice.call(new ClientMessage.ListRooms(id()));
        alice.call(playRoom());
        assertTrue(alice.call(new ClientMessage.SendChat(id(), null, "hello lobby")).isOk());
        kit.lobby.awaitIdle();

        RouterKit.Client late = kit.hello("late");
        RoomList list = (RoomList) late.call(new ClientMessage.ListRooms(id())).payload();
        assertEquals(1, list.rooms().size());
        assertTrue(list.recentLobbyChat().stream().anyMatch(m -> m.text().equals("hello lobby") && !m.system()));
        assertTrue(list.recentLobbyChat().stream().anyMatch(m -> m.system() && m.text().contains("alice created")));
    }

    @Test @DisplayName("lobby chat needs the lobby open, validates text and is rate limited")
    void lobbyChatRules() {
        RouterKit.Client alice = kit.hello("alice");
        assertEquals(ErrorCode.NOT_IN_ROOM,
                alice.call(new ClientMessage.SendChat(id(), null, "anyone?")).errorCode());
        alice.call(new ClientMessage.ListRooms(id()));
        assertEquals(ErrorCode.CHAT_INVALID, alice.call(new ClientMessage.SendChat(id(), null, "  ")).errorCode());
        for (int i = 0; i < 5; i++) {
            assertTrue(alice.call(new ClientMessage.SendChat(id(), null, "msg " + i)).isOk());
        }
        assertEquals(ErrorCode.CHAT_RATE_LIMITED,
                alice.call(new ClientMessage.SendChat(id(), null, "msg 6")).errorCode());
    }

    @Test @DisplayName("room chat sent through the router reaches the room")
    void roomChatThroughRouter() {
        RouterKit.Client alice = kit.hello("alice");
        String roomId = (String) alice.call(playRoom()).payload();
        assertTrue(alice.call(new ClientMessage.SendChat(id(), roomId, "hi room")).isOk());
        ChatMessage chat = alice.inbox.await(ChatMessage.class, m -> !m.system() && m.text().equals("hi room"), 5000);
        assertEquals(roomId, chat.roomId());
    }

    @Test @DisplayName("disconnecting frees the nickname, leaves the lobby and hands the person's seat to the AI")
    void disconnectCleansUp() {
        RouterKit.Client alice = kit.hello("alice");
        RouterKit.Client bob = kit.hello("bob");
        String roomId = (String) alice.call(playRoom()).payload();
        bob.call(new ClientMessage.JoinRoom(id(), roomId));
        alice.call(new ClientMessage.ClaimSeat(id(), roomId, com.ludot.protocol.Colour.RED));
        alice.call(new ClientMessage.ListRooms(id()));
        alice.call(new ClientMessage.StartGame(id(), roomId));

        kit.router.disconnected(alice.session);
        kit.router.disconnected(alice.session); // idempotent

        bob.inbox.await(ServerMessage.SeatsUpdated.class, s -> s.seats().stream().anyMatch(x ->
                x.colour() == com.ludot.protocol.Colour.RED
                        && x.controller() == com.ludot.protocol.Controller.DISCONNECTED), 5000);
        assertTrue(kit.connectAndHello("alice"));
    }

    @Test @DisplayName("rooms are numbered in order and a closed room leaves the registry and the lobby")
    void emptyRoomLeavesRegistry() throws InterruptedException {
        RouterKit fast = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1).withEmptyRoomCloseMs(50));
        try {
            RouterKit.Client alice = fast.hello("alice");
            RouterKit.Client watcher = fast.hello("watcher");
            watcher.call(new ClientMessage.ListRooms(id()));
            String first = (String) alice.call(playRoom()).payload();
            String second = (String) alice.call(playRoom()).payload();
            assertEquals("R-0001", first);
            assertEquals("R-0002", second);

            alice.call(new ClientMessage.LeaveRoom(id(), first));
            Kit.await(() -> fast.registry.find(first) == null, 5000);
            fast.lobby.awaitIdle();
            ServerMessage.LobbyUpdate last = watcher.inbox.of(ServerMessage.LobbyUpdate.class).stream()
                    .reduce((a, b) -> b).get();
            assertEquals(List.of(second), last.rooms().stream().map(RoomSummary::roomId).toList());
        } finally {
            fast.close();
        }
    }
}
