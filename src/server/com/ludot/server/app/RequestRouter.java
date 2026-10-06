package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends each request from a connection where it belongs: identity requests to the directory, lobby
 * requests to the lobby, room requests to that room's queue. It runs on the connection's reader
 * thread, does almost no work itself and never blocks, so one slow request cannot hold up others.
 */
public final class RequestRouter {

    private interface Handler {
        void handle(ClientSession session, ClientMessage message);
    }

    private final PlayerDirectory directory;
    private final RoomRegistry registry;
    private final LobbyService lobby;
    private final GameRepository repository;
    private final GameHistory history;
    private final ReplayService replays;
    private final Map<Class<?>, Handler> handlers = new HashMap<>();

    public RequestRouter(PlayerDirectory directory, RoomRegistry registry, LobbyService lobby,
                         GameRepository repository) {
        this(directory, registry, lobby, repository, GameHistory.NONE, null);
    }

    /** @param replays null when replays are not offered (for instance in small tests) */
    public RequestRouter(PlayerDirectory directory, RoomRegistry registry, LobbyService lobby,
                         GameRepository repository, GameHistory history, ReplayService replays) {
        this.history = history;
        this.replays = replays;
        this.repository = repository;
        this.directory = directory;
        this.registry = registry;
        this.lobby = lobby;
        handlers.put(ClientMessage.Ping.class, (s, m) -> s.sink().offer(new ServerMessage.Pong(System.currentTimeMillis())));
        handlers.put(ClientMessage.Hello.class, (s, m) -> hello(s, (ClientMessage.Hello) m));
        handlers.put(ClientMessage.Reconnect.class, (s, m) -> reconnect(s, (ClientMessage.Reconnect) m));
        handlers.put(ClientMessage.SignOut.class, (s, m) -> signOut(s, (ClientMessage.SignOut) m));
        handlers.put(ClientMessage.ListRooms.class, (s, m) -> listRooms(s, (ClientMessage.ListRooms) m));
        handlers.put(ClientMessage.CreateRoom.class, (s, m) -> createRoom(s, (ClientMessage.CreateRoom) m));
        handlers.put(ClientMessage.SendChat.class, (s, m) -> sendChat(s, (ClientMessage.SendChat) m));
        handlers.put(ClientMessage.ListFinishedGames.class, (s, m) -> listFinished(s, (ClientMessage.ListFinishedGames) m));
        handlers.put(ClientMessage.StartReplay.class, (s, m) -> startReplay(s, (ClientMessage.StartReplay) m));
        handlers.put(ClientMessage.ControlReplay.class, (s, m) -> controlReplay(s, (ClientMessage.ControlReplay) m));
    }

    /**
     * Routes one message from a connection.
     *
     * @return false if the connection broke the protocol and should be closed
     */
    public boolean route(ClientSession session, ClientMessage message) {
        if (!session.isIdentified() && !isAllowedBeforeHello(message)) {
            reject(session, message, ErrorCode.PROTOCOL_ERROR, "Say Hello first");
            return false;
        }
        if (message instanceof ClientMessage.Request request) {
            Response earlier = session.requests().find(idOf(session), request.requestId());
            if (earlier != null) {
                session.sink().offer(earlier);
                return true;
            }
        }
        Handler handler = handlers.get(message.getClass());
        if (handler != null) {
            handler.handle(session, message);
        } else if (message instanceof ClientMessage.RoomRequest room) {
            toRoom(session, room);
        } else {
            reject(session, message, ErrorCode.PROTOCOL_ERROR, "Unknown message");
            return false;
        }
        return true;
    }

    /** Cleans up after a connection ends for any reason. Safe to call from any thread, any number of times. */
    public void disconnected(ClientSession session) {
        if (!session.markDisconnected()) {
            return;
        }
        Participant who = session.participant();
        if (who == null) {
            return;
        }
        lobby.unsubscribe(who.id());
        for (String roomId : session.rooms()) {
            GameSession room = registry.find(roomId);
            if (room != null) {
                room.connectionLost(who.id(), session.sink());
            }
        }
        if (replays != null) {
            replays.stop(who.id());
        }
        directory.detach(who.identity(), session); // frees the nickname now, or after the grace period if it may reconnect
    }

    private static boolean isAllowedBeforeHello(ClientMessage message) {
        return message instanceof ClientMessage.Hello || message instanceof ClientMessage.Reconnect
                || message instanceof ClientMessage.Ping;
    }

    // before Hello there is no identity, so no per-player log; the one allowed request (Hello) has no earlier answer
    private static String idOf(ClientSession session) {
        return session.isIdentified() ? session.participant().id() : "";
    }

    private void hello(ClientSession session, ClientMessage.Hello request) {
        if (session.isIdentified()) {
            respond(session, Response.error(request.requestId(), ErrorCode.PROTOCOL_ERROR, "Already said Hello"));
            return;
        }
        try {
            PlayerIdentity identity = directory.register(request.nickname(), request.reconnectable());
            session.identify(identity);
            directory.attach(identity, session);
            repository.record(new PlayerRecord(identity.id(), identity.nickname()));
            session.sink().offer(new ServerMessage.Welcome(identity.id(), identity.nickname(), identity.token()));
            respond(session, Response.ok(request.requestId()));
        } catch (RequestRejected rejected) {
            respond(session, Response.error(request.requestId(), rejected.code(), rejected.getMessage()));
        }
    }

    // a new connection claims the identity behind a token (see PlayerDirectory.reattach); the rooms are re-entered by the client
    private void reconnect(ClientSession session, ClientMessage.Reconnect request) {
        if (session.isIdentified()) {
            respond(session, Response.error(request.requestId(), ErrorCode.PROTOCOL_ERROR, "Already identified"));
            return;
        }
        java.util.Optional<PlayerIdentity> found = directory.reattach(request.token(), session);
        if (found.isEmpty()) {
            respond(session, Response.error(request.requestId(), ErrorCode.UNKNOWN_TOKEN,
                    "That session has expired: please sign in again"));
            return;
        }
        PlayerIdentity identity = found.get();
        session.identify(identity);
        session.sink().offer(new ServerMessage.Welcome(identity.id(), identity.nickname(), identity.token()));
        respond(session, Response.ok(request.requestId()));
    }

    // a deliberate sign-out frees the nickname at once; rooms are cleaned up when the connection closes
    private void signOut(ClientSession session, ClientMessage.SignOut request) {
        directory.release(session.participant().identity());
        respond(session, Response.ok(request.requestId()));
    }

    // reads from storage on this connection's own reader thread, so a slow database never delays a room or the lobby
    private void listFinished(ClientSession session, ClientMessage.ListFinishedGames request) {
        try {
            respond(session, Response.ok(request.requestId(), history.finished(request.page())));
        } catch (HistoryException e) {
            respond(session, Response.error(request.requestId(), ErrorCode.INTERNAL_ERROR, e.getMessage()));
        }
    }

    private void startReplay(ClientSession session, ClientMessage.StartReplay request) {
        if (replays == null) {
            unsupported(session, request, ErrorCode.INTERNAL_ERROR, "Replays are not available");
            return;
        }
        replays.start(session.participant(), request, response -> respond(session, response));
    }

    private void controlReplay(ClientSession session, ClientMessage.ControlReplay request) {
        if (replays == null) {
            unsupported(session, request, ErrorCode.INTERNAL_ERROR, "Replays are not available");
            return;
        }
        respond(session, replays.control(session.participant(), request));
    }

    private void listRooms(ClientSession session, ClientMessage.ListRooms request) {
        if (!lobby.subscribe(session.participant(), request)) {
            respond(session, Response.error(request.requestId(), ErrorCode.SERVER_BUSY, "Lobby is busy"));
        }
    }

    private void createRoom(ClientSession session, ClientMessage.CreateRoom request) {
        Participant creator = session.participant();
        GameSession room = registry.create(request.mode(), request.variant(), request.seed());
        session.rooms().add(room.roomId());
        // the response goes first, then the room sends the creator's snapshot
        respond(session, Response.ok(request.requestId(), room.roomId()));
        room.attachCreator(creator);
        String kind = request.mode() == RoomMode.BOT_MATCH ? "Bot match" : "Play room";
        lobby.announce(creator.identity().nickname() + " created " + kind + " " + room.roomId());
    }

    private void sendChat(ClientSession session, ClientMessage.SendChat request) {
        if (request.roomId() == null) {
            if (!lobby.sendChat(session.participant(), request)) {
                respond(session, Response.error(request.requestId(), ErrorCode.SERVER_BUSY, "Lobby is busy"));
            }
        } else {
            toRoom(session, request.roomId(), request);
        }
    }

    private void toRoom(ClientSession session, ClientMessage.RoomRequest request) {
        if (request instanceof ClientMessage.JoinRoom) {
            session.rooms().add(request.roomId());
        }
        toRoom(session, request.roomId(), request);
    }

    private void toRoom(ClientSession session, String roomId, ClientMessage.Request request) {
        GameSession room = registry.find(roomId);
        if (room == null) {
            respond(session, Response.error(request.requestId(), ErrorCode.UNKNOWN_ROOM, "No such room: " + roomId));
            return;
        }
        room.submit(session.participant(), request);
    }

    private void unsupported(ClientSession session, ClientMessage.Request request, ErrorCode code, String why) {
        respond(session, Response.error(request.requestId(), code, why));
    }

    // answers on the reader thread are remembered here so a repeated request id gets the same answer
    private void respond(ClientSession session, Response response) {
        session.requests().remember(idOf(session), response);
        session.sink().offer(response);
    }

    private void reject(ClientSession session, ClientMessage message, ErrorCode code, String why) {
        if (message instanceof ClientMessage.Request request) {
            session.sink().offer(Response.error(request.requestId(), code, why));
        }
    }
}
