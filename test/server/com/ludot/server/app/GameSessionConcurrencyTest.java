package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The race conditions of the room actor, each fired from many threads at once behind a start gate. */
@DisplayName("GameSession concurrency")
class GameSessionConcurrencyTest {

    private Kit kit;
    private ExecutorService pool;

    @BeforeEach void setUp() {
        kit = new Kit();
        pool = Executors.newCachedThreadPool();
    }

    @AfterEach void tearDown() {
        pool.shutdownNow();
        kit.close();
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    /** Runs every task at the same moment and returns their results. */
    private <T> List<T> fireTogether(List<Callable<T>> tasks) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        return results;
    }

    // CT-01
    @RepeatedTest(20) @DisplayName("50 simultaneous claims on one seat: exactly one wins, 49 get SEAT_TAKEN")
    void seatClaimRace() throws Exception {
        Participant host = Kit.person("host");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, host);
        List<Participant> contenders = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Participant p = Kit.person("p" + i);
            Kit.call(room, p, Kit.join(room));
            contenders.add(p);
        }

        List<Callable<Response>> claims = new ArrayList<>();
        for (Participant p : contenders) {
            claims.add(() -> Kit.call(room, p, new ClientMessage.ClaimSeat(id(), room.roomId(), Colour.RED)));
        }
        List<Response> responses = fireTogether(claims);

        assertEquals(1, responses.stream().filter(Response::isOk).count());
        assertEquals(49, responses.stream().filter(r -> r.errorCode() == ErrorCode.SEAT_TAKEN).count());

        Participant winner = contenders.get(responses.indexOf(responses.stream().filter(Response::isOk).findFirst().get()));
        Kit.call(room, host, new ClientMessage.RequestSnapshot(id(), room.roomId()));
        ServerMessage.RoomSnapshot snapshot = Kit.sink(host).of(ServerMessage.RoomSnapshot.class).stream()
                .reduce((a, b) -> b).get();
        assertTrue(snapshot.seats().stream().anyMatch(s -> s.colour() == Colour.RED
                && s.controller() == Controller.HUMAN && s.playerId().equals(winner.id())));
    }

    // CT-04
    @RepeatedTest(20) @DisplayName("ten threads sending the same request id: applied once, ten identical answers")
    void duplicateRequestIds() throws Exception {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);
        ClientMessage.ClaimSeat claim = new ClientMessage.ClaimSeat(id(), room.roomId(), Colour.GREEN);

        List<Callable<Boolean>> sends = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sends.add(() -> {
                room.submit(alice, claim);
                return true;
            });
        }
        fireTogether(sends);
        Kit.settle(room, alice);

        List<Response> answers = Kit.sink(alice).of(Response.class).stream()
                .filter(r -> r.requestId().equals(claim.requestId())).toList();
        assertEquals(10, answers.size());
        assertTrue(answers.stream().allMatch(a -> a.equals(answers.get(0))));
        assertTrue(answers.get(0).isOk());
        long claimBroadcasts = Kit.sink(alice).of(ServerMessage.SeatsUpdated.class).stream()
                .filter(s -> s.seats().stream().anyMatch(x -> x.controller() == Controller.HUMAN)).count();
        assertEquals(1, claimBroadcasts);
    }

    // CT-05
    @RepeatedTest(10) @DisplayName("joining while the AI floods updates: every joiner's pushes continue exactly after its snapshot")
    void joinUnderLoad() throws Exception {
        Participant host = Kit.person("host");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);

        List<Participant> joiners = new ArrayList<>();
        List<Callable<Response>> joins = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Participant p = Kit.person("j" + i);
            joiners.add(p);
            joins.add(() -> Kit.call(room, p, Kit.join(room)));
        }
        fireTogether(joins);

        for (Participant joiner : joiners) {
            RecordingSink sink = Kit.sink(joiner);
            ServerMessage.RoomSnapshot snapshot = sink.await(ServerMessage.RoomSnapshot.class, 5000);
            Kit.await(() -> pushSeqsAfterSnapshot(sink).size() >= 20
                    || !sink.of(ServerMessage.GameOver.class).isEmpty(), 20_000);
            long expected = snapshot.seq() + 1;
            for (long seq : pushSeqsAfterSnapshot(sink)) {
                assertEquals(expected++, seq, "gap or repeat after snapshot at " + snapshot.seq());
            }
        }
    }

    private static List<Long> pushSeqsAfterSnapshot(RecordingSink sink) {
        List<Long> seqs = new ArrayList<>();
        boolean after = false;
        for (ServerMessage message : sink.messages()) {
            if (message instanceof ServerMessage.RoomSnapshot) {
                after = true;
            } else if (after && message instanceof ServerMessage.RoomPush push) {
                seqs.add(push.seq());
            }
        }
        return seqs;
    }

    // CT-07
    @Test @DisplayName("a flood into a full queue is answered SERVER_BUSY instead of growing without bound")
    void backpressure() throws Exception {
        kit.config = SessionConfig.defaults().withDelays(5, 5, 5).withQueueCapacity(100);

        // the creator's sink blocks the room thread on its very first message, so nothing is processed
        CountDownLatch release = new CountDownLatch(1);
        RecordingSink inner = new RecordingSink();
        ClientSink blocking = new ClientSink() {
            private final AtomicBoolean first = new AtomicBoolean(true);

            @Override
            public boolean offer(ServerMessage message) {
                if (first.getAndSet(false)) {
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return inner.offer(message);
            }

            @Override
            public void close() {
                inner.close();
            }
        };
        Participant creator = new Participant(new PlayerIdentity("id-creator", "creator", "t"), blocking);
        GameSession room = kit.room(RoomMode.PLAY, 1);
        room.attachCreator(creator);
        Kit.await(() -> room.queueDepth() == 0, 5000);

        Participant flooder = Kit.person("flooder");
        for (int i = 0; i < 1000; i++) {
            room.submit(flooder, new ClientMessage.RequestSnapshot(id(), room.roomId()));
        }
        long busy = Kit.sink(flooder).of(Response.class).stream()
                .filter(r -> r.errorCode() == ErrorCode.SERVER_BUSY).count();
        assertTrue(busy >= 900, "expected at least 900 SERVER_BUSY, got " + busy);
        assertTrue(room.queueDepth() <= 100, "queue must stay bounded");

        release.countDown();
        Kit.await(() -> Kit.sink(flooder).of(Response.class).size() == 1000, 10_000);
    }

    // CT-10
    @RepeatedTest(5) @DisplayName("ten chatters during an AI flood: all five listeners see the identical, gap-free sequence")
    void chatTotalOrdering() throws Exception {
        kit.config = SessionConfig.defaults().withDelays(0, 0, 0);
        Participant host = Kit.person("host");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 7, host);

        List<Participant> listeners = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Participant p = Kit.person("listener" + i);
            Kit.call(room, p, Kit.join(room));
            listeners.add(p);
        }
        List<Participant> chatters = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Participant p = Kit.person("chatter" + i);
            Kit.call(room, p, Kit.join(room));
            chatters.add(p);
        }

        List<Callable<Boolean>> talk = new ArrayList<>();
        for (Participant chatter : chatters) {
            talk.add(() -> {
                for (int n = 0; n < 5; n++) {
                    Response r = Kit.call(room, chatter, new ClientMessage.SendChat(id(), room.roomId(),
                            chatter.identity().nickname() + "#" + n));
                    assertTrue(r.isOk(), String.valueOf(r.message()));
                }
                return true;
            });
        }
        fireTogether(talk);

        for (Participant listener : listeners) {
            Kit.await(() -> chatTexts(Kit.sink(listener)).size() >= 50, 20_000);
        }

        List<List<Long>> sequences = new ArrayList<>();
        for (Participant listener : listeners) {
            sequences.add(pushSeqsAfterSnapshot(Kit.sink(listener)));
        }
        long start = sequences.stream().mapToLong(s -> s.get(0)).max().getAsLong();
        long end = sequences.stream().mapToLong(s -> s.get(s.size() - 1)).min().getAsLong();
        List<Long> reference = window(sequences.get(0), start, end);
        assertFalse(reference.isEmpty());
        for (long i = 0; i < reference.size(); i++) {
            assertEquals(reference.get(0) + i, reference.get((int) i), "gap in the shared sequence");
        }
        for (List<Long> sequence : sequences) {
            assertEquals(reference, window(sequence, start, end));
        }

        List<String> expectedChat = chatTexts(Kit.sink(listeners.get(0)));
        for (Participant listener : listeners) {
            assertEquals(expectedChat, chatTexts(Kit.sink(listener)), "chat order differs between listeners");
        }
        assertEquals(50, expectedChat.size());
    }

    private static List<Long> window(List<Long> sequence, long from, long to) {
        return sequence.stream().filter(s -> s >= from && s <= to).toList();
    }

    private static List<String> chatTexts(RecordingSink sink) {
        return sink.of(ChatMessage.class).stream().filter(m -> !m.system()).map(ChatMessage::text).toList();
    }

    // CT-11
    @RepeatedTest(10) @DisplayName("twenty simultaneous messages from one player: exactly five accepted; a message after the window is accepted")
    void chatRateLimiter() throws Exception {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 1, alice);

        List<Callable<Response>> burst = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            int n = i;
            burst.add(() -> {
                ClientMessage.SendChat chat = new ClientMessage.SendChat(id(), room.roomId(), "burst " + n);
                room.submit(alice, chat);
                return Kit.sink(alice).responseTo(chat.requestId());
            });
        }
        List<Response> responses = fireTogether(burst);

        assertEquals(5, responses.stream().filter(Response::isOk).count());
        assertEquals(15, responses.stream().filter(r -> r.errorCode() == ErrorCode.CHAT_RATE_LIMITED).count());

        kit.clock.addAndGet(5_000);
        assertTrue(Kit.call(room, alice, new ClientMessage.SendChat(id(), room.roomId(), "later")).isOk());
    }

    // CT-02
    @RepeatedTest(20) @DisplayName("a timeout and the player's own roll racing for the same turn: exactly one decision is recorded for it")
    void timeoutVersusRoll() throws Exception {
        kit.config = SessionConfig.defaults().withDelays(0, 1, 1).withTurnTimeoutMs(3_600_000);
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        Kit.call(room, alice, new ClientMessage.ClaimSeat(id(), room.roomId(), Colour.RED));
        Kit.call(room, alice, new ClientMessage.StartGame(id(), room.roomId()));
        ServerMessage.YourTurn turn = Kit.sink(alice).await(ServerMessage.YourTurn.class,
                t -> t.phase() == com.ludot.protocol.Phase.AWAITING_ROLL, 10_000);

        ClientMessage.Roll roll = new ClientMessage.Roll(id(), room.roomId(), turn.turnNumber());
        List<Callable<Object>> racers = new ArrayList<>();
        racers.add(() -> {
            room.submit(alice, roll);
            return null;
        });
        racers.add(() -> {
            room.injectTimeoutForTest(turn.turnNumber());
            return null;
        });
        fireTogether(racers);
        Kit.settle(room, alice);

        List<com.ludot.server.app.DecisionRecord> atThatTurn = kit.repository.decisions().stream()
                .filter(d -> d.turnNumber() == turn.turnNumber()).toList();
        assertEquals(1, atThatTurn.size(), "exactly one decision for the turn: " + atThatTurn);
        Response answer = Kit.sink(alice).responseTo(roll.requestId());
        if (atThatTurn.get(0).source() == com.ludot.server.app.DecisionSource.HUMAN) {
            assertTrue(answer.isOk());
        } else {
            assertEquals(com.ludot.server.app.DecisionSource.TIMEOUT, atThatTurn.get(0).source());
            assertFalse(answer.isOk(), "the late roll must be refused, not applied a second time");
        }
    }

    // CT-03
    @RepeatedTest(20) @DisplayName("pausing mid-flood: no game update is ever applied after the pause notice")
    void pauseVersusAi() {
        kit.config = SessionConfig.defaults().withDelays(1, 1, 1);
        Participant host = Kit.person("host");
        Participant watcher = Kit.person("watcher");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);
        Kit.call(room, watcher, Kit.join(room));

        Kit.sleep(ThreadLocalRandom.current().nextInt(30));
        assertTrue(Kit.call(room, host, new ClientMessage.PauseGame(id(), room.roomId())).isOk());
        ServerMessage.RoomStateChanged paused = Kit.sink(watcher).await(ServerMessage.RoomStateChanged.class,
                ServerMessage.RoomStateChanged::paused, 5000);

        Kit.sleep(100);
        long late = Kit.sink(watcher).of(ServerMessage.StateUpdate.class).stream()
                .filter(u -> u.seq() > paused.seq()).count();
        assertEquals(0, late, "an AI action ran after the game was paused");
    }
}
