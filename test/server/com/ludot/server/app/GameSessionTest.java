package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.Phase;
import com.ludot.protocol.Role;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Speed;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameSession (room actor)")
class GameSessionTest {

    private Kit kit;

    @BeforeEach void setUp() {
        kit = new Kit();
    }

    @AfterEach void tearDown() {
        kit.close();
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    private static void assertRejected(ErrorCode expected, Response response) {
        assertEquals(ErrorCode.valueOf(expected.name()), response.errorCode(), String.valueOf(response.message()));
        assertFalse(response.isOk());
    }

    private static Response claim(GameSession room, Participant who, Colour colour) {
        return Kit.call(room, who, new ClientMessage.ClaimSeat(id(), room.roomId(), colour));
    }

    private static Response start(GameSession room, Participant who) {
        return Kit.call(room, who, new ClientMessage.StartGame(id(), room.roomId()));
    }

    // plays a human seat by answering every YourTurn, until the game ends
    private static void playToTheEnd(GameSession room, Participant human, long timeoutMs) {
        RecordingSink sink = Kit.sink(human);
        int cursor = 0;
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            List<ServerMessage> all = sink.messages();
            for (; cursor < all.size(); cursor++) {
                ServerMessage message = all.get(cursor);
                if (message instanceof ServerMessage.GameOver) {
                    return;
                }
                if (message instanceof ServerMessage.YourTurn turn) {
                    ClientMessage.Request move = turn.phase() == Phase.AWAITING_ROLL
                            ? new ClientMessage.Roll(id(), room.roomId(), turn.turnNumber())
                            : new ClientMessage.ChooseMove(id(), room.roomId(), turn.turnNumber(), 0);
                    room.submit(human, move);
                }
            }
            Kit.sleep(1);
        }
        List<ServerMessage> all = sink.messages();
        fail("game did not finish in time; room=" + room.summary() + " messages=" + all.size() + " last="
                + all.subList(Math.max(0, all.size() - 4), all.size()));
    }

    // ---------------------------------------------------------------- membership

    @Test @DisplayName("the creator becomes host and receives a snapshot of a waiting room")
    void creatorIsHost() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        ServerMessage.RoomSnapshot snapshot = Kit.sink(alice).of(ServerMessage.RoomSnapshot.class).get(0);
        assertEquals(RoomStatus.WAITING, snapshot.status());
        assertEquals(alice.id(), snapshot.hostId());
        assertEquals(16, snapshot.board().pieces().size());
        Kit.settle(room, alice);
        assertEquals("alice", room.summary().hostNickname());
    }

    @Test @DisplayName("joining answers OK, then sends the snapshot, and tells the others")
    void joinOrdersResponseThenSnapshot() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Response joined = Kit.call(room, bob, Kit.join(room));
        assertTrue(joined.isOk());
        Kit.sink(bob).await(ServerMessage.RoomSnapshot.class, 5000);
        List<ServerMessage> bobSaw = Kit.sink(bob).messages();
        assertInstanceOf(Response.class, bobSaw.get(0));
        assertInstanceOf(ServerMessage.RoomSnapshot.class, bobSaw.get(1));

        ServerMessage.MembersChanged changed = Kit.sink(alice).await(ServerMessage.MembersChanged.class,
                m -> m.members().size() == 2, 5000);
        assertTrue(changed.members().stream().anyMatch(m -> m.nickname().equals("bob") && m.role() == Role.SPECTATOR));
    }

    @Test @DisplayName("a stranger cannot act in a room")
    void nonMemberRejected() {
        Participant alice = Kit.person("alice");
        Participant mallory = Kit.person("mallory");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        assertRejected(ErrorCode.NOT_IN_ROOM, claim(room, mallory, Colour.RED));
        assertRejected(ErrorCode.NOT_IN_ROOM, Kit.call(room, mallory,
                new ClientMessage.SendChat(id(), room.roomId(), "hi")));
    }

    @Test @DisplayName("when the host leaves, the longest-present member takes over and a leaver's seat is freed")
    void hostHandover() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        Participant carol = Kit.person("carol");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Kit.call(room, bob, Kit.join(room));
        Kit.call(room, carol, Kit.join(room));
        claim(room, alice, Colour.RED);

        assertTrue(Kit.call(room, alice, new ClientMessage.LeaveRoom(id(), room.roomId())).isOk());

        ServerMessage.RoomStateChanged changed = Kit.sink(bob).await(ServerMessage.RoomStateChanged.class,
                s -> s.hostId().equals(bob.id()), 5000);
        assertEquals(bob.id(), changed.hostId());
        ServerMessage.SeatsUpdated seats = Kit.sink(bob).await(ServerMessage.SeatsUpdated.class,
                s -> s.seats().stream().allMatch(seat -> seat.controller() == Controller.FREE), 5000);
        assertNotNull(seats);
    }

    // ---------------------------------------------------------------- seats

    @Test @DisplayName("a seat claim is broadcast, a second claim on it is refused, and a player can switch seats")
    void claimingSeats() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Kit.call(room, bob, Kit.join(room));

        assertTrue(claim(room, alice, Colour.RED).isOk());
        assertRejected(ErrorCode.SEAT_TAKEN, claim(room, bob, Colour.RED));
        Kit.sink(bob).await(ServerMessage.SeatsUpdated.class,
                s -> s.seats().stream().anyMatch(x -> x.colour() == Colour.RED && x.controller() == Controller.HUMAN), 5000);

        assertTrue(claim(room, alice, Colour.BLUE).isOk());
        assertTrue(claim(room, bob, Colour.RED).isOk());
    }

    @Test @DisplayName("releasing a seat frees it; releasing without one is refused")
    void releasingSeats() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        assertRejected(ErrorCode.NOT_A_PLAYER, Kit.call(room, alice, new ClientMessage.ReleaseSeat(id(), room.roomId())));
        claim(room, alice, Colour.GREEN);
        assertTrue(Kit.call(room, alice, new ClientMessage.ReleaseSeat(id(), room.roomId())).isOk());
        assertTrue(claim(room, alice, Colour.GREEN).isOk());
    }

    @Test @DisplayName("only the host can start, and only with at least one human seat")
    void startRules() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Kit.call(room, bob, Kit.join(room));
        assertRejected(ErrorCode.NOT_HOST, start(room, bob));
        assertRejected(ErrorCode.NO_HUMAN_SEATS, start(room, alice));
    }

    @Test @DisplayName("starting fills empty seats with AI and locks the seats")
    void startFillsWithAiAndLocks() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Kit.call(room, bob, Kit.join(room));
        claim(room, alice, Colour.RED);
        assertTrue(start(room, alice).isOk());

        ServerMessage.SeatsUpdated seats = Kit.sink(bob).await(ServerMessage.SeatsUpdated.class,
                s -> s.seats().stream().filter(x -> x.controller() == Controller.AI).count() == 3, 5000);
        assertNotNull(seats);
        assertRejected(ErrorCode.SEATS_LOCKED, claim(room, bob, Colour.BLUE));
        assertRejected(ErrorCode.SEATS_LOCKED, start(room, alice));
    }

    // ---------------------------------------------------------------- gameplay validation

    @Test @DisplayName("gameplay is refused before the game starts")
    void gameplayBeforeStart() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        claim(room, alice, Colour.RED);
        assertRejected(ErrorCode.GAME_NOT_RUNNING,
                Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), 0)));
    }

    // starts a game where alice holds RED and waits until it is a human turn, returning the YourTurn
    private ServerMessage.YourTurn startAndWaitForHumanTurn(GameSession room, Participant alice, Participant bob) {
        claim(room, alice, Colour.RED);
        start(room, alice);
        return Kit.sink(alice).await(ServerMessage.YourTurn.class, 10_000);
    }

    @Test @DisplayName("a spectator cannot play; a wrong turn number is stale; the wrong phase is refused")
    void validationOrder() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        Kit.call(room, bob, Kit.join(room));
        ServerMessage.YourTurn turn = startAndWaitForHumanTurn(room, alice, bob);

        assertRejected(ErrorCode.NOT_A_PLAYER,
                Kit.call(room, bob, new ClientMessage.Roll(id(), room.roomId(), turn.turnNumber())));
        assertRejected(ErrorCode.STALE_TURN,
                Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), turn.turnNumber() + 7)));
        assertRejected(ErrorCode.WRONG_PHASE,
                Kit.call(room, alice, new ClientMessage.ChooseMove(id(), room.roomId(), turn.turnNumber(), 0)));
    }

    @Test @DisplayName("a roll returns the dice value, then the move phase rejects a bad index and accepts a good one")
    void rollThenMove() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        ServerMessage.YourTurn turn = startAndWaitForHumanTurn(room, alice, alice);
        assertEquals(Phase.AWAITING_ROLL, turn.phase());

        // early in a game most rolls leave nothing to decide (the AI skips for us), so roll until a move is offered
        ServerMessage.YourTurn moveTurn = null;
        int lastTurn = -1;
        for (int attempt = 0; attempt < 200 && moveTurn == null; attempt++) {
            final int after = lastTurn;
            ServerMessage.YourTurn next = Kit.sink(alice).await(ServerMessage.YourTurn.class,
                    t -> t.turnNumber() > after, 10_000);
            lastTurn = next.turnNumber();
            if (next.phase() == Phase.AWAITING_MOVE) {
                moveTurn = next;
                break;
            }
            Response rolled = Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), next.turnNumber()));
            assertTrue(rolled.isOk());
            int value = (Integer) rolled.payload();
            assertTrue(value >= 1 && value <= 6);
        }
        assertNotNull(moveTurn, "never offered a move");
        assertNotNull(moveTurn.roll());
        ServerMessage.YourTurn offered = moveTurn;
        assertRejected(ErrorCode.INVALID_MOVE, Kit.call(room, alice,
                new ClientMessage.ChooseMove(id(), room.roomId(), offered.turnNumber(), offered.options().size())));
        assertRejected(ErrorCode.INVALID_MOVE, Kit.call(room, alice,
                new ClientMessage.ChooseMove(id(), room.roomId(), offered.turnNumber(), -1)));
        assertTrue(Kit.call(room, alice,
                new ClientMessage.ChooseMove(id(), room.roomId(), offered.turnNumber(), 0)).isOk());
        assertTrue(kit.repository.decisions().stream().anyMatch(d -> d.source() == DecisionSource.HUMAN && d.optionIndex() != null));
    }

    @Test @DisplayName("a human and three AI seats play a whole game to GameOver")
    void humanPlaysFullGame() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        claim(room, alice, Colour.RED);
        start(room, alice);
        playToTheEnd(room, alice, 60_000);

        ServerMessage.GameOver over = Kit.sink(alice).of(ServerMessage.GameOver.class).get(0);
        assertEquals(4, over.standings().size());
        Kit.settle(room, alice);
        assertEquals(RoomStatus.FINISHED, room.summary().status());
        assertFalse(kit.repository.decisions().isEmpty());
    }

    // ---------------------------------------------------------------- bot match and host controls

    @Test @DisplayName("a bot match starts by itself and every spectator sees the identical sequence to the end")
    void botMatchSpectatorsAgree() {
        Participant host = Kit.person("host");
        Participant s1 = Kit.person("s1");
        Participant s2 = Kit.person("s2");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);
        Kit.call(room, s1, Kit.join(room));
        Kit.call(room, s2, Kit.join(room));
        Kit.sink(s1).await(ServerMessage.GameOver.class, 60_000);
        Kit.sink(s2).await(ServerMessage.GameOver.class, 60_000);

        List<Long> seq1 = pushSeqsAfterSnapshot(s1);
        List<Long> seq2 = pushSeqsAfterSnapshot(s2);
        assertFalse(seq1.isEmpty());
        // s2 joined after s1, so compare from the later of the two starting points
        long from = Math.max(seq1.get(0), seq2.get(0));
        assertEquals(seq1.stream().filter(s -> s >= from).toList(), seq2.stream().filter(s -> s >= from).toList());
        long lastHash1 = Kit.sink(s1).of(ServerMessage.StateUpdate.class).stream().reduce((a, b) -> b).get().stateHash();
        long lastHash2 = Kit.sink(s2).of(ServerMessage.StateUpdate.class).stream().reduce((a, b) -> b).get().stateHash();
        assertEquals(lastHash1, lastHash2);
    }

    // sequence numbers of every room push that arrived after the snapshot, in arrival order
    private static List<Long> pushSeqsAfterSnapshot(Participant who) {
        List<Long> seqs = new ArrayList<>();
        boolean afterSnapshot = false;
        for (ServerMessage message : Kit.sink(who).messages()) {
            if (message instanceof ServerMessage.RoomSnapshot) {
                afterSnapshot = true;
            } else if (afterSnapshot && message instanceof ServerMessage.RoomPush push) {
                seqs.add(push.seq());
            }
        }
        return seqs;
    }

    @Test @DisplayName("room pushes after a snapshot follow each other with no gap or repeat")
    void sequenceIsGapFree() {
        Participant host = Kit.person("host");
        Participant spectator = Kit.person("spectator");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 3, host);
        Kit.call(room, spectator, Kit.join(room));
        Kit.sink(spectator).await(ServerMessage.GameOver.class, 60_000);

        ServerMessage.RoomSnapshot snapshot = Kit.sink(spectator).of(ServerMessage.RoomSnapshot.class).get(0);
        long expected = snapshot.seq() + 1;
        for (long seq : pushSeqsAfterSnapshot(spectator)) {
            assertEquals(expected++, seq);
        }
    }

    @Test @DisplayName("pausing stops all game updates; resuming continues; only the host may do either")
    void pauseAndResume() {
        kit.config = SessionConfig.defaults().withDelays(5, 5, 5);
        Participant host = Kit.person("host");
        Participant spectator = Kit.person("spectator");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);
        Kit.call(room, spectator, Kit.join(room));

        assertRejected(ErrorCode.NOT_HOST, Kit.call(room, spectator, new ClientMessage.PauseGame(id(), room.roomId())));
        assertTrue(Kit.call(room, host, new ClientMessage.PauseGame(id(), room.roomId())).isOk());
        Kit.sink(spectator).await(ServerMessage.RoomStateChanged.class, ServerMessage.RoomStateChanged::paused, 5000);
        Kit.settle(room, host);

        int updatesWhenPaused = Kit.sink(spectator).of(ServerMessage.StateUpdate.class).size();
        assertThrows(AssertionError.class, () -> Kit.await(
                () -> Kit.sink(spectator).of(ServerMessage.StateUpdate.class).size() > updatesWhenPaused, 150));
        assertRejected(ErrorCode.GAME_PAUSED, Kit.call(room, host, new ClientMessage.PauseGame(id(), room.roomId())));

        assertTrue(Kit.call(room, host, new ClientMessage.ResumeGame(id(), room.roomId())).isOk());
        Kit.await(() -> Kit.sink(spectator).of(ServerMessage.StateUpdate.class).size() > updatesWhenPaused, 5000);
    }

    @Test @DisplayName("the host can change the speed and everyone is told")
    void speedChange() {
        Participant host = Kit.person("host");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);
        assertTrue(Kit.call(room, host, new ClientMessage.SetSpeed(id(), room.roomId(), Speed.SLOW)).isOk());
        Kit.sink(host).await(ServerMessage.RoomStateChanged.class, s -> s.speed() == Speed.SLOW, 5000);
    }

    @Test @DisplayName("an empty bot match pauses itself and a room with nobody left closes")
    void emptyRoomPausesAndCloses() {
        kit.config = SessionConfig.defaults().withDelays(5, 5, 5).withEmptyRoomCloseMs(100);
        Participant host = Kit.person("host");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);
        Kit.call(room, host, new ClientMessage.LeaveRoom(id(), room.roomId()));
        Kit.await(() -> kit.lobby.closed.contains(room.roomId()), 5000);
        assertEquals(RoomStatus.CLOSED, room.summary().status());
    }

    // ---------------------------------------------------------------- duplicates, disconnects, slow clients

    @Test @DisplayName("a repeated request id returns the original response and is applied once")
    void duplicateRequestIdIsApplyOnce() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        ClientMessage.ClaimSeat claim = new ClientMessage.ClaimSeat(id(), room.roomId(), Colour.RED);
        Response first = Kit.call(room, alice, claim);
        room.submit(alice, claim);
        Kit.settle(room, alice);
        List<Response> answers = Kit.sink(alice).of(Response.class).stream()
                .filter(r -> r.requestId().equals(claim.requestId())).toList();
        assertEquals(2, answers.size());
        assertEquals(first, answers.get(1));
        assertTrue(first.isOk());
        long claimBroadcasts = Kit.sink(alice).of(ServerMessage.SeatsUpdated.class).stream()
                .filter(s -> s.seats().stream().anyMatch(x -> x.controller() == Controller.HUMAN)).count();
        assertEquals(1, claimBroadcasts);
    }

    @Test @DisplayName("a repeated id is still recognised after hundreds of other requests (the memory outlasts a flood)")
    void duplicateRecognisedAfterManyRequests() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        ClientMessage.ClaimSeat claim = new ClientMessage.ClaimSeat(id(), room.roomId(), Colour.RED);
        assertTrue(Kit.call(room, alice, claim).isOk());
        for (int i = 0; i < 500; i++) {
            room.submit(alice, new ClientMessage.RequestSnapshot(id(), room.roomId()));
        }
        // re-evaluating the claim would answer SEAT_TAKEN (she already holds RED); the remembered answer is OK
        room.submit(alice, new ClientMessage.ClaimSeat(claim.requestId(), room.roomId(), Colour.RED));
        Kit.settle(room, alice);
        List<Response> answers = Kit.sink(alice).of(Response.class).stream()
                .filter(r -> r.requestId().equals(claim.requestId())).toList();
        assertEquals(2, answers.size());
        assertTrue(answers.get(1).isOk(), String.valueOf(answers.get(1)));
    }

    @Test @DisplayName("a dropped connection hands the seat to the AI, which keeps playing it")
    void connectionLostHandsSeatToAi() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Kit.call(room, bob, Kit.join(room));
        claim(room, alice, Colour.RED);
        start(room, alice);
        Kit.sink(bob).await(ServerMessage.StateUpdate.class, 5000);

        room.connectionLost(alice.id(), alice.sink());
        ServerMessage.SeatsUpdated detached = Kit.sink(bob).await(ServerMessage.SeatsUpdated.class,
                s -> s.seats().stream().anyMatch(x -> x.colour() == Colour.RED && x.controller() == Controller.DISCONNECTED), 5000);

        // the AI now rolls for RED even though nobody is attached to that seat
        ServerMessage.StateUpdate redActs = Kit.sink(bob).await(ServerMessage.StateUpdate.class,
                u -> u.seq() > detached.seq() && u.events().stream()
                        .anyMatch(e -> e.type() == com.ludot.protocol.EventType.DICE_ROLL && e.colour() == Colour.RED),
                20_000);
        assertNotNull(redActs);
        Kit.settle(room, bob);
        assertEquals(1, room.summary().spectators());
    }

    @Test @DisplayName("a slow client whose queue fills is dropped without hurting the others or the room")
    void slowConsumerIsDropped() {
        Participant host = Kit.person("host");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);
        Participant slow = new Participant(new PlayerIdentity("id-slow", "slow", "t"), new RecordingSink(3));
        Kit.call(room, slow, Kit.join(room));
        Kit.sink(host).await(ServerMessage.GameOver.class, 60_000);

        assertTrue(Kit.sink(slow).isClosed());
        Kit.settle(room, host);
        assertTrue(Kit.sink(host).of(ServerMessage.MembersChanged.class).stream()
                .anyMatch(m -> m.members().size() == 1 && m.members().get(0).playerId().equals(host.id())));
    }

    // ---------------------------------------------------------------- chat

    @Test @DisplayName("chat is cleaned, validated, broadcast with a sequence number and stored")
    void chatBasics() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Kit.call(room, bob, Kit.join(room));
        claim(room, alice, Colour.RED);

        assertTrue(Kit.call(room, alice, new ClientMessage.SendChat(id(), room.roomId(), "  hello\tworld \n")).isOk());
        ServerMessage.ChatMessage received = Kit.sink(bob).await(ServerMessage.ChatMessage.class,
                m -> !m.system() && m.text().startsWith("hello"), 5000);
        assertEquals("hello world", received.text());
        assertEquals(Role.PLAYER, received.role());
        assertEquals(Colour.RED, received.seatColour());
        assertTrue(kit.repository.chats().stream().anyMatch(c -> c.text().equals("hello world") && !c.system()));

        assertRejected(ErrorCode.CHAT_INVALID, Kit.call(room, bob, new ClientMessage.SendChat(id(), room.roomId(), "   ")));
        assertRejected(ErrorCode.CHAT_INVALID,
                Kit.call(room, bob, new ClientMessage.SendChat(id(), room.roomId(), "x".repeat(201))));
    }

    @Test @DisplayName("a spectator's chat is tagged as a spectator")
    void spectatorChatTag() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        Kit.call(room, bob, Kit.join(room));
        Kit.call(room, bob, new ClientMessage.SendChat(id(), room.roomId(), "hi"));
        ServerMessage.ChatMessage received = Kit.sink(alice).await(ServerMessage.ChatMessage.class,
                m -> !m.system() && m.text().equals("hi"), 5000);
        assertEquals(Role.SPECTATOR, received.role());
        assertNull(received.seatColour());
    }

    @Test @DisplayName("a late joiner's snapshot carries the most recent 30 chat messages")
    void snapshotCarriesChatTail() {
        Participant alice = Kit.person("alice");
        Participant late = Kit.person("late");
        kit.config = SessionConfig.defaults().withDelays(0, 1, 1);
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        for (int i = 0; i < 40; i++) {
            kit.clock.addAndGet(10_000); // keep clear of the rate limit
            Kit.call(room, alice, new ClientMessage.SendChat(id(), room.roomId(), "message " + i));
        }
        Kit.call(room, late, Kit.join(room));
        ServerMessage.RoomSnapshot snapshot = Kit.sink(late).await(ServerMessage.RoomSnapshot.class, 5000);
        assertEquals(30, snapshot.recentChat().size());
        assertEquals("message 39", snapshot.recentChat().get(29).text());
    }
}
