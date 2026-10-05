package com.ludot.bot;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.MessageChannel;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A headless connection to the server for the automatic clients. One reader thread decodes
 * everything the server sends: Responses complete the future of the request they answer, other
 * messages go to listeners. Requests are asynchronous: {@link #request} returns at once with a
 * future that times out after five seconds, so many requests can be in flight on one connection.
 */
public final class ServerLink implements AutoCloseable {

    private static final long REQUEST_TIMEOUT_SECONDS = 5;

    private final Socket socket;
    private final MessageChannel<ClientMessage, ServerMessage> channel;
    // a queue per id so a deliberately repeated request id can have two answers outstanding
    private final ConcurrentHashMap<UUID, Queue<CompletableFuture<Response>>> pending = new ConcurrentHashMap<>();
    private final List<Consumer<ServerMessage>> listeners = new CopyOnWriteArrayList<>();
    private final CompletableFuture<Void> closed = new CompletableFuture<>();
    private volatile ScheduledExecutorService heartbeat;

    private ServerLink(Socket socket) throws IOException {
        this.socket = socket;
        this.channel = MessageChannel.forClient(socket);
    }

    public static ServerLink connect(String host, int port) throws IOException {
        ServerLink link = new ServerLink(new Socket(host, port));
        Thread reader = new Thread(link::readLoop, "link-reader");
        reader.setDaemon(true);
        reader.start();
        return link;
    }

    /** Registers a listener for every message that is not a Response. It runs on the reader thread, so keep it quick. */
    public void addListener(Consumer<ServerMessage> listener) {
        listeners.add(listener);
    }

    /** Sends a request without waiting; the future completes with the Response, or exceptionally on timeout or a lost connection. */
    public CompletableFuture<Response> request(ClientMessage.Request request) {
        CompletableFuture<Response> future = new CompletableFuture<Response>()
                .orTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        pending.computeIfAbsent(request.requestId(), id -> new ConcurrentLinkedQueue<>()).add(future);
        future.whenComplete((response, error) -> forget(request.requestId(), future));
        try {
            channel.send(request);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /** Sends a message that has no response (a Ping). */
    public void send(ClientMessage message) throws IOException {
        channel.send(message);
    }

    /** Pings the server regularly so it does not treat this connection as dead. */
    public void startHeartbeat(long intervalMs) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "link-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(this::ping, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        heartbeat = executor;
    }

    private void ping() {
        try {
            channel.send(new ClientMessage.Ping());
        } catch (IOException e) {
            close();
        }
    }

    public boolean isClosed() {
        return closed.isDone();
    }

    /** Completes when the connection ends, for any reason. */
    public CompletableFuture<Void> whenClosed() {
        return closed;
    }

    private void readLoop() {
        try {
            while (true) {
                dispatch(channel.receive());
            }
        } catch (IOException e) {
            failAll(e);
        }
    }

    private void dispatch(ServerMessage message) {
        if (message instanceof Response response) {
            Queue<CompletableFuture<Response>> waiting = pending.get(response.requestId());
            CompletableFuture<Response> future = waiting == null ? null : waiting.poll();
            if (future != null) {
                future.complete(response);
            }
            return;
        }
        for (Consumer<ServerMessage> listener : listeners) {
            listener.accept(message);
        }
    }

    private void forget(UUID id, CompletableFuture<Response> future) {
        pending.computeIfPresent(id, (key, waiting) -> {
            waiting.remove(future);
            return waiting.isEmpty() ? null : waiting;
        });
    }

    private void failAll(IOException cause) {
        closed.complete(null);
        pending.values().forEach(waiting -> waiting.forEach(f -> f.completeExceptionally(cause)));
    }

    @Override
    public void close() {
        ScheduledExecutorService executor = heartbeat;
        if (executor != null) {
            executor.shutdownNow();
        }
        try {
            socket.close();
        } catch (IOException e) {
            // already closed
        }
        closed.complete(null);
    }
}
