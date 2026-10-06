package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.ServerMessage.Response;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import com.ludot.protocol.Speed;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Starts and controls replays of finished games. Each person has at most one replay at a time; it belongs to their
 * connection and ends when that connection does.
 */
public final class ReplayService {

    private final GameHistory history;
    private final ScheduledExecutorService scheduler;
    private final Function<Speed, Long> delays;
    private final Map<String, Replay> byPlayer = new ConcurrentHashMap<>();
    private final AtomicInteger numbers = new AtomicInteger();

    /** Pause between replayed steps: much quicker than live play, since the point is to skim a whole game. */
    public static long defaultDelayMs(Speed speed) {
        return switch (speed) {
            case FAST -> 25;
            case NORMAL -> 150;
            case SLOW -> 600;
        };
    }

    public ReplayService(GameHistory history, ScheduledExecutorService scheduler) {
        this(history, scheduler, ReplayService::defaultDelayMs);
    }

    /** @param delays the pause between steps at each speed (tests make it tiny) */
    public ReplayService(GameHistory history, ScheduledExecutorService scheduler, Function<Speed, Long> delays) {
        this.history = history;
        this.scheduler = scheduler;
        this.delays = delays;
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
        Replay replay = new Replay(replayId, data.get(), who.sink(), request.speed(), scheduler, delays,
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
