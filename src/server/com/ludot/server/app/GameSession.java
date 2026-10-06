package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.Variant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * One room, run as an actor. A private single-thread mailbox ({@link RoomActor}) processes every command for
 * the room one at a time, in arrival order: requests from connection threads, timers and AI actions all enter
 * as queued commands, so all room state is confined to that one thread and needs no locks. The queue is the
 * "put requests in a queue for processing as soon as possible" the assignment asks for; when it is full the
 * sender gets SERVER_BUSY.
 *
 * <p>Every response is sent before the pushes it caused, and every push to the room carries the next
 * sequence number, so each member sees the same totally ordered stream, chat included.
 *
 * <p>This class only wires the parts together and exposes the thread-safe entry points. Each part owns one job:
 * {@link Membership} (who is here), {@link Seating} (who plays which colour), {@link GamePlay} (starting, rolling,
 * moving, pausing), {@link TurnDriver} (what happens next and the clocks), {@link RoomChat}, and
 * {@link Broadcaster} (everything sent to members). They share one {@link RoomState}.
 */
public final class GameSession {

    private final RoomState state;
    private final RoomActor actor;
    private final Broadcaster broadcaster;
    private final Membership membership;
    private final GamePlay gamePlay;
    private final TurnDriver turns;

    public GameSession(String roomId, RoomMode mode, Variant variant, Long seed, SessionConfig config,
                       ScheduledExecutorService scheduler, LobbyPublisher lobby, GameRepository repository,
                       LongSupplier clock) {
        long resolvedSeed = seed != null ? seed : ThreadLocalRandom.current().nextLong();
        this.state = new RoomState(roomId, mode, variant, resolvedSeed, config, clock);
        Map<Class<?>, RequestHandler> handlers = new HashMap<>();
        this.actor = new RoomActor(roomId, config, scheduler, handlers);
        RoomGuards guards = new RoomGuards(state);
        this.broadcaster = new Broadcaster(state, lobby);
        RoomChat chat = new RoomChat(state, guards, broadcaster, repository);
        this.turns = new TurnDriver(state, broadcaster, chat, actor, repository);
        this.membership = new Membership(state, guards, broadcaster, chat, turns, actor, repository, actor::shutdown);
        this.gamePlay = new GamePlay(state, guards, broadcaster, chat, turns, repository);
        Seating seating = new Seating(state, guards, broadcaster, chat, turns);

        membership.register(handlers);
        seating.register(handlers);
        gamePlay.register(handlers);
        chat.register(handlers);
        actor.afterEachCommand(this::dropSlowMembers);
        state.summary = broadcaster.buildSummary();
    }

    // ------------------------------------------------------------------ entry points (any thread)

    public String roomId() {
        return state.roomId;
    }

    /** The latest summary published by the room thread. */
    public RoomSummary summary() {
        return state.summary;
    }

    /** Commands waiting for the room thread; logged by the server to show queueing. */
    public int queueDepth() {
        return actor.queueDepth();
    }

    /**
     * Queues a request from a connection thread. If the room's queue is full the sender is told
     * SERVER_BUSY immediately instead of letting memory grow without limit.
     */
    public void submit(Participant from, ClientMessage.Request request) {
        actor.submit(from, request);
    }

    /** Makes the creator the first member and host; a Bot match starts straight away. */
    public void attachCreator(Participant creator) {
        actor.submitInternal(() -> {
            membership.addCreator(creator);
            if (state.mode == RoomMode.BOT_MATCH) {
                gamePlay.beginGame();
            }
            broadcaster.publishSummary();
        });
    }

    /** The connection dropped (not a polite leave); a seat held by that person is handed to the AI. */
    public void connectionLost(String playerId, ClientSink sink) {
        actor.submitInternal(() -> membership.connectionLost(playerId, sink));
    }

    /** Stops the room: no more commands are accepted and timers are cancelled. */
    public void shutdown() {
        turns.cancelTimers();
        membership.cancelCloseTimer();
        actor.shutdownNow();
    }

    /** Waits until everything submitted so far has run, including the summaries it sent to the lobby; for tests. */
    void awaitIdle() throws InterruptedException {
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        actor.submitInternal(done::countDown);
        if (!done.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new IllegalStateException("The room did not catch up within 10 s");
        }
    }

    /** Test hook: behaves exactly as if the deadline for that turn had just passed. */
    void injectTimeoutForTest(int turn) {
        actor.submitInternal(() -> turns.onTurnTimeout(turn));
    }

    // ------------------------------------------------------------------ on the room thread

    // a member whose outbound queue overflowed was closed by the broadcaster; remove them now, repeating
    // because removing someone broadcasts, which may overflow another queue
    private void dropSlowMembers() {
        for (List<Member> dead = broadcaster.takeSlowConsumers(); !dead.isEmpty();
                dead = broadcaster.takeSlowConsumers()) {
            for (Member member : dead) {
                if (state.members.containsKey(member.id())) {
                    membership.remove(member);
                }
            }
        }
    }
}
