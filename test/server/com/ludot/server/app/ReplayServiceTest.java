package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.ReplayAction;
import com.ludot.protocol.ReplayEndReason;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ReplayEnded;
import com.ludot.protocol.ServerMessage.ReplayEvent;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ReplayService: playing a finished game again")
class ReplayServiceTest {

    private static ReplayFixtures.Played played;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final RecordingSink sink = new RecordingSink();
    private final Participant viewer = new Participant(new PlayerIdentity("viewer-id", "viewer", "token"), sink);
    private final List<Response> responses = new ArrayList<>();

    @BeforeAll
    static void playOneGame() {
        played = ReplayFixtures.play("R-0001", Variant.LUDO_T, 1L);
    }

    @AfterEach
    void stop() {
        scheduler.shutdownNow();
    }

    private static GameHistory historyOf(ReplayData... games) {
        return new GameHistory() {
            @Override
            public com.ludot.protocol.FinishedGameList finished(int page) {
                return new com.ludot.protocol.FinishedGameList(List.of(), page, false);
            }

            @Override
            public Optional<ReplayData> load(String gameId) {
                return List.of(games).stream().filter(g -> g.gameId().equals(gameId)).findFirst();
            }
        };
    }

    // steps are paced by SessionConfig.delayMs: FAST 0 ms, NORMAL 20 ms
    private ReplayService service(ReplayData... games) {
        return new ReplayService(historyOf(games), scheduler, SessionConfig.defaults().withDelays(0, 20, 20));
    }

    private String start(ReplayService service, String gameId, Speed speed) {
        service.start(viewer, new ClientMessage.StartReplay(UUID.randomUUID(), gameId, speed), response -> {
            // the answer must come before the first step of this replay
            if (response.isOk()) {
                assertTrue(sink.of(ReplayEvent.class).stream().noneMatch(e -> e.replayId().equals(response.payload())),
                        "a step arrived before the answer");
            }
            responses.add(response);
        });
        return responses.isEmpty() || !responses.get(responses.size() - 1).isOk() ? null
                : (String) responses.get(responses.size() - 1).payload();
    }

    private ReplayEnded awaitEnd() {
        return sink.await(ReplayEnded.class, 20_000);
    }

    @Test @DisplayName("a stored game replays to the end: same winner, same final board, every step numbered")
    void completes() {
        ReplayService service = service(played.data());
        String id = start(service, "R-0001", Speed.FAST);
        assertEquals(1, responses.size());
        assertEquals("RP-1", id);

        ReplayEnded ended = awaitEnd();
        assertEquals(ReplayEndReason.COMPLETED, ended.reason());
        List<ReplayEvent> events = sink.of(ReplayEvent.class);
        int total = played.data().steps().size();
        assertEquals(total + 1, events.size(), "the opening position plus one event per decision");
        for (int i = 0; i < events.size(); i++) {
            assertEquals(i, events.get(i).index());
            assertEquals(total, events.get(i).total());
            assertEquals(id, events.get(i).replayId());
        }
        assertEquals(played.finalBoard(), events.get(events.size() - 1).board());
        assertEquals(0, service.running(), "a finished replay is forgotten");
    }

    @Test @DisplayName("the opening position is sent first and carries events of its own")
    void startsWithTheOpening() {
        ReplayService service = service(played.data());
        start(service, "R-0001", Speed.FAST);
        ReplayEvent first = sink.await(ReplayEvent.class, 5_000);
        assertEquals(0, first.index());
        assertFalse(first.events().isEmpty(), "the roll-off and first turn should be narrated");
        awaitEnd();
    }

    @Test @DisplayName("a roll that comes out differently from the record ends the replay as DIVERGED")
    void tamperedRollDiverges() {
        List<ReplayData.Step> steps = new ArrayList<>(played.data().steps());
        int at = 0;
        while (steps.get(at).optionIndex() != null || steps.get(at).roll() == null) {
            at++;
        }
        ReplayData.Step original = steps.get(at);
        steps.set(at, new ReplayData.Step(original.turnNumber(), original.colour(), original.roll() % 6 + 1, null));
        ReplayData tampered = new ReplayData("R-0001", Variant.LUDO_T, 1L, played.winner(), steps);

        ReplayService service = service(tampered);
        start(service, "R-0001", Speed.FAST);
        assertEquals(ReplayEndReason.DIVERGED, awaitEnd().reason());
    }

    @Test @DisplayName("a move the engine refuses ends the replay as DIVERGED")
    void impossibleMoveDiverges() {
        List<ReplayData.Step> steps = new ArrayList<>(played.data().steps());
        int at = 0;
        while (steps.get(at).optionIndex() == null) {
            at++;
        }
        ReplayData.Step original = steps.get(at);
        steps.set(at, new ReplayData.Step(original.turnNumber(), original.colour(), null, 99));
        ReplayService service = service(new ReplayData("R-0001", Variant.LUDO_T, 1L, played.winner(), steps));
        start(service, "R-0001", Speed.FAST);
        assertEquals(ReplayEndReason.DIVERGED, awaitEnd().reason());
    }

    @Test @DisplayName("a record that stops before the game is over is not passed off as complete")
    void truncatedRecordDiverges() {
        List<ReplayData.Step> shortened = played.data().steps().subList(0, played.data().steps().size() - 5);
        ReplayService service = service(new ReplayData("R-0001", Variant.LUDO_T, 1L, played.winner(), shortened));
        start(service, "R-0001", Speed.FAST);
        assertEquals(ReplayEndReason.DIVERGED, awaitEnd().reason());
    }

    @Test @DisplayName("pause holds the replay still, resume carries on, stop ends it")
    void pauseResumeStop() throws Exception {
        ReplayService service = service(played.data());
        String id = start(service, "R-0001", Speed.NORMAL);
        sink.await(ReplayEvent.class, e -> e.index() >= 3, 5_000);

        Response paused = service.control(viewer, new ClientMessage.ControlReplay(UUID.randomUUID(), id, ReplayAction.PAUSE, null));
        assertTrue(paused.isOk());
        int frozen = sink.of(ReplayEvent.class).size();
        Thread.sleep(250); // ten steps' worth of time
        assertEquals(frozen, sink.of(ReplayEvent.class).size(), "nothing may arrive while paused");

        assertTrue(service.control(viewer, new ClientMessage.ControlReplay(UUID.randomUUID(), id, ReplayAction.RESUME, null)).isOk());
        sink.await(ReplayEvent.class, e -> e.index() >= frozen + 3, 5_000);

        assertTrue(service.control(viewer, new ClientMessage.ControlReplay(UUID.randomUUID(), id, ReplayAction.STOP, null)).isOk());
        assertEquals(ReplayEndReason.STOPPED, awaitEnd().reason());
        int after = sink.of(ReplayEvent.class).size();
        Thread.sleep(150);
        assertEquals(after, sink.of(ReplayEvent.class).size(), "nothing may arrive after stopping");
        assertEquals(0, service.running());
    }

    @Test @DisplayName("starting a second replay ends the first: one replay per person")
    void secondReplayReplacesTheFirst() {
        ReplayService service = service(played.data());
        String first = start(service, "R-0001", Speed.NORMAL);
        sink.await(ReplayEvent.class, 5_000);
        String second = start(service, "R-0001", Speed.NORMAL);

        assertFalse(first.equals(second));
        ReplayEnded ended = sink.await(ReplayEnded.class, e -> e.replayId().equals(first), 5_000);
        assertEquals(ReplayEndReason.STOPPED, ended.reason());
        assertEquals(1, service.running());
        service.stop(viewer.id());
    }

    @Test @DisplayName("stopping a person's replay when their connection closes")
    void connectionClosedStopsIt() {
        ReplayService service = service(played.data());
        start(service, "R-0001", Speed.NORMAL);
        sink.await(ReplayEvent.class, 5_000);
        service.stop(viewer.id());
        assertEquals(ReplayEndReason.STOPPED, awaitEnd().reason());
        assertEquals(0, service.running());
    }

    @Test @DisplayName("a game that does not exist is refused with UNKNOWN_ROOM and nothing starts")
    void unknownGame() {
        ReplayService service = service(played.data());
        assertEquals(null, start(service, "R-9999", Speed.FAST));
        assertEquals(ErrorCode.UNKNOWN_ROOM, responses.get(0).errorCode());
        assertEquals(0, service.running());
    }

    @Test @DisplayName("controlling a replay that is not running, or someone else's, is refused")
    void controlOfNothing() {
        ReplayService service = service(played.data());
        Response refused = service.control(viewer, new ClientMessage.ControlReplay(UUID.randomUUID(), "RP-42", ReplayAction.PAUSE, null));
        assertEquals(ErrorCode.UNKNOWN_ROOM, refused.errorCode());
    }

    @Test @DisplayName("a viewer that cannot keep up simply loses the replay; nothing else is affected")
    void slowViewerEndsTheReplay() throws Exception {
        List<ServerMessage> accepted = new ArrayList<>();
        ClientSink choking = new ClientSink() {
            @Override
            public boolean offer(ServerMessage message) {
                if (accepted.size() < 5) {
                    accepted.add(message);
                    return true;
                }
                return false;
            }

            @Override
            public void close() {
            }
        };
        Participant slow = new Participant(new PlayerIdentity("slow-id", "slow", "t"), choking);
        ReplayService service = service(played.data());
        service.start(slow, new ClientMessage.StartReplay(UUID.randomUUID(), "R-0001", Speed.FAST), responses::add);
        for (int i = 0; i < 200 && service.running() > 0; i++) {
            Thread.sleep(10);
        }
        assertEquals(0, service.running(), "the replay should have been dropped");
    }
}
