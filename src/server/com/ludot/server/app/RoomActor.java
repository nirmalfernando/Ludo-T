package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.server.concurrent.NamedThreadFactory;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The room's mailbox: a private single-thread executor with a bounded queue. Every request, timer and AI
 * action for the room is a command in this queue and runs on this one thread, one at a time, in arrival
 * order. That confinement is why the rest of the room needs no locks, and the bound is why a flood gets
 * SERVER_BUSY instead of unbounded memory growth.
 */
final class RoomActor {

    private static final Logger LOG = Logger.getLogger(RoomActor.class.getName());
    private static final long RETRY_DELAY_MS = 20;

    private final String roomId;
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService scheduler;
    private final Map<Class<?>, RequestHandler> handlers;
    private final RequestLog requests;
    private Runnable afterEachCommand = () -> { };

    RoomActor(String roomId, SessionConfig config, ScheduledExecutorService scheduler,
              Map<Class<?>, RequestHandler> handlers) {
        this.roomId = roomId;
        this.scheduler = scheduler;
        this.handlers = handlers;
        this.requests = new RequestLog(config.requestMemory());
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(config.queueCapacity()),
                new NamedThreadFactory("room-" + roomId, false), new ThreadPoolExecutor.AbortPolicy());
    }

    /** Work to do on the room thread after every command, however it ended (the room drops slow clients here). */
    void afterEachCommand(Runnable work) {
        this.afterEachCommand = work;
    }

    int queueDepth() {
        return executor.getQueue().size();
    }

    /**
     * Queues a request from a connection thread. If the queue is full the sender is told SERVER_BUSY at once
     * instead of letting memory grow without limit.
     */
    void submit(Participant from, ClientMessage.Request request) {
        try {
            executor.execute(() -> run(() -> handle(from, request)));
        } catch (RejectedExecutionException e) {
            ErrorCode code = executor.isShutdown() ? ErrorCode.UNKNOWN_ROOM : ErrorCode.SERVER_BUSY;
            from.sink().offer(Response.error(request.requestId(), code, "Room cannot take requests right now"));
        }
    }

    /** Queues work from a timer or the room itself; if the queue is full it tries again shortly rather than stall the game. */
    void submitInternal(Runnable command) {
        try {
            executor.execute(() -> run(command));
        } catch (RejectedExecutionException e) {
            if (!executor.isShutdown()) {
                schedule(() -> submitInternal(command), RETRY_DELAY_MS);
            }
        }
    }

    /** Runs the task after the delay. Returns null when the scheduler has stopped, which means the server is shutting down. */
    ScheduledFuture<?> schedule(Runnable task, long delayMs) {
        try {
            return scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null;
        }
    }

    static void cancel(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
    }

    /** Stops accepting commands but lets the ones already queued finish. */
    void shutdown() {
        executor.shutdown();
    }

    /** Stops now, discarding queued commands. */
    void shutdownNow() {
        executor.shutdownNow();
    }

    private void run(Runnable command) {
        try {
            command.run();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected failure in room " + roomId, e);
        } finally {
            afterEachCommand.run();
        }
    }

    // a repeated request id gets the original answer; otherwise the handler runs, its response goes out, then its follow-up
    private void handle(Participant from, ClientMessage.Request request) {
        Response earlier = requests.find(from.id(), request.requestId());
        if (earlier != null) {
            from.sink().offer(earlier);
            return;
        }
        Reply reply = answer(from, request);
        requests.remember(from.id(), reply.response());
        from.sink().offer(reply.response());
        if (reply.followUp() != null) {
            reply.followUp().run();
        }
    }

    private Reply answer(Participant from, ClientMessage.Request request) {
        try {
            return handlers.get(request.getClass()).handle(from, request);
        } catch (RequestRejected rejected) {
            return new Reply(Response.error(request.requestId(), rejected.code(), rejected.getMessage()), null);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Request failed in room " + roomId + ": " + request, e);
            return new Reply(Response.error(request.requestId(), ErrorCode.INTERNAL_ERROR, "Internal error"), null);
        }
    }
}
