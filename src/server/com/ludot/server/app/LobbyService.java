package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomList;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.server.concurrent.NamedThreadFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * The lobby as a second actor: one thread ("lobby") owns the room list, the subscribers and the lobby
 * chat, so room changes and chat messages share a single order (the lobby sequence number). Rooms
 * publish their summaries here; whoever is subscribed sees them live.
 *
 * <p>Updates from rooms are never dropped. Requests from clients (subscribe, chat) are refused with
 * SERVER_BUSY when the lobby is already far behind, so a flood cannot starve the room updates.
 */
public final class LobbyService implements LobbyPublisher, AutoCloseable {

    private static final int CLIENT_BACKLOG_LIMIT = 10_000;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new NamedThreadFactory("lobby", false));

    // confined to the lobby thread
    private final Map<String, RoomSummary> rooms = new TreeMap<>();
    private final Map<String, Participant> subscribers = new LinkedHashMap<>();
    private final ChatChannel chat;
    private final RequestLog requests;
    private final GameRepository repository;
    private long lobbySeq;

    public LobbyService(SessionConfig config, LongSupplier clock, GameRepository repository) {
        this.repository = repository;
        this.chat = new ChatChannel(config, clock);
        this.requests = new RequestLog(config.requestMemory());
    }

    // ------------------------------------------------------------------ from rooms (never dropped)

    @Override
    public void roomChanged(RoomSummary summary) {
        post(() -> {
            rooms.put(summary.roomId(), summary);
            broadcastRooms();
        });
    }

    @Override
    public void roomClosed(String roomId) {
        post(() -> {
            if (rooms.remove(roomId) != null) {
                broadcastRooms();
                // the chat also said "created room ...", so say when it is gone instead of leaving that line dangling
                publishChat(null, "system", "Room " + roomId + " was closed", true);
            }
        });
    }

    /** Posts a system line to lobby chat, e.g. "alice created room R-0007". */
    public void announce(String text) {
        post(() -> publishChat(null, "system", text, true));
    }

    // ------------------------------------------------------------------ from clients (bounded)

    /** Subscribes the person to live lobby updates and answers with the current room list and recent chat. */
    public boolean subscribe(Participant who, ClientMessage.ListRooms request) {
        return postFromClient(() -> {
            subscribers.put(who.id(), who);
            RoomList list = new RoomList(new ArrayList<>(rooms.values()), lobbySeq, chat.recent());
            deliver(who, Response.ok(request.requestId(), list));
        });
    }

    public void unsubscribe(String playerId) {
        post(() -> subscribers.remove(playerId));
    }

    /** Lobby chat; the sender must be subscribed (that is, viewing the lobby). */
    public boolean sendChat(Participant who, ClientMessage.SendChat request) {
        return postFromClient(() -> {
            Response earlier = requests.find(who.id(), request.requestId());
            if (earlier != null) {
                deliver(who, earlier);
                return;
            }
            Response response = chatResponse(who, request);
            requests.remember(who.id(), response);
            deliver(who, response);
            if (response.isOk()) {
                publishChat(who.id(), who.identity().nickname(), chat.clean(request.text()), false);
            }
        });
    }

    private Response chatResponse(Participant who, ClientMessage.SendChat request) {
        try {
            if (!subscribers.containsKey(who.id())) {
                throw new RequestRejected(ErrorCode.NOT_IN_ROOM, "Open the lobby to chat in it");
            }
            chat.clean(request.text());
            chat.admit(who.id());
            return Response.ok(request.requestId());
        } catch (RequestRejected rejected) {
            return Response.error(request.requestId(), rejected.code(), rejected.getMessage());
        }
    }

    // ------------------------------------------------------------------ lobby thread

    private void publishChat(String senderId, String nickname, String text, boolean system) {
        ChatMessage message = new ChatMessage(null, ++lobbySeq, senderId, nickname, null, null, text,
                chat.now(), system);
        chat.remember(message);
        repository.record(new ChatRecord(null, message.seq(), senderId, text, system, message.sentAt()));
        broadcast(message);
    }

    private void broadcastRooms() {
        broadcast(new ServerMessage.LobbyUpdate(++lobbySeq, new ArrayList<>(rooms.values())));
    }

    private void broadcast(ServerMessage message) {
        List<Participant> snapshot = new ArrayList<>(subscribers.values());
        for (Participant subscriber : snapshot) {
            deliver(subscriber, message);
        }
    }

    // a subscriber that cannot keep up is dropped from the lobby and its connection closed
    private void deliver(Participant who, ServerMessage message) {
        if (!who.sink().offer(message)) {
            subscribers.remove(who.id());
            who.sink().close();
        }
    }

    private void post(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            // lobby is shut down; nothing left to update
        }
    }

    private boolean postFromClient(Runnable task) {
        if (executor.getQueue().size() >= CLIENT_BACKLOG_LIMIT) {
            return false;
        }
        post(task);
        return true;
    }

    /** Waits until everything posted so far has run; for tests that need a stable view. */
    public void awaitIdle() throws InterruptedException {
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        post(done::countDown);
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The lobby did not catch up within 10 s");
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
