package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.ServerMessage.Response;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Starts and controls replays of finished games. Each person has at most one replay at a time; it belongs to their
 * connection and ends when that connection does.
 */
public final class ReplayService {

    private final GameHistory history;
    private final ScheduledExecutorService scheduler;
    private final SessionConfig config;
    private final Map<String, Replay> byPlayer = new ConcurrentHashMap<>();
    private final AtomicInteger numbers = new AtomicInteger();

    public ReplayService(GameHistory history, ScheduledExecutorService scheduler, SessionConfig config) {
        this.history = history;
        this.scheduler = scheduler;
        this.config = config;
    }

    /**
     * Answers the request through {@code respond} and only then starts sending steps, so the viewer always sees the
     * answer (with the replay id) before the first step.
     */
    public void start(Participant who, ClientMessage.StartReplay request, Consumer<Response> respond) {
        Optional<ReplayData> data;
        try {
            data = history.load(request.gameId());
        } catch (HistoryException e) {
            respond.accept(Response.error(request.requestId(), ErrorCode.INTERNAL_ERROR, e.getMessage()));
            return;
        }
        if (data.isEmpty()) {
            respond.accept(Response.error(request.requestId(), ErrorCode.UNKNOWN_ROOM,
                    "There is no finished game " + request.gameId()));
            return;
        }
        stop(who.id()); // one replay per person: starting another ends the first
        String replayId = "RP-" + numbers.incrementAndGet();
        Replay replay = new Replay(replayId, data.get(), who.sink(), request.speed(), scheduler, config::delayMs,
                ended -> byPlayer.remove(who.id(), ended));
        byPlayer.put(who.id(), replay);
        respond.accept(Response.ok(request.requestId(), replayId));
        replay.begin();
    }

    public Response control(Participant who, ClientMessage.ControlReplay request) {
        Replay replay = byPlayer.get(who.id());
        if (replay == null || !replay.id().equals(request.replayId())) {
            return Response.error(request.requestId(), ErrorCode.UNKNOWN_ROOM, "That replay is not running");
        }
        if (request.speed() != null) {
            replay.setSpeed(request.speed());
        }
        switch (request.action()) {
            case PAUSE -> replay.pause();
            case RESUME -> replay.resume();
            case STOP -> replay.stop();
        }
        return Response.ok(request.requestId());
    }

    /** Ends the person's replay, if any (their connection closed, or they started another). */
    public void stop(String playerId) {
        Replay replay = byPlayer.remove(playerId);
        if (replay != null) {
            replay.stop();
        }
    }

    /** Replays running right now (for tests and the queue log). */
    public int running() {
        return byPlayer.size();
    }
}
