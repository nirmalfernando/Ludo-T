package com.ludot.client.net;

import com.ludot.client.state.ClientState;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.ServerLink;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * The client's connection to the server. Everything the server pushes is handed to the FX thread
 * before it touches {@link ClientState}, and every answer to a request is delivered on the FX thread
 * too, so the UI never blocks on the network and never races with the reader thread.
 */
public final class Session implements AutoCloseable {

    private static final long CONNECT_TIMEOUT_SECONDS = 8;
    private static final long HEARTBEAT_MS = 5_000;
    private static final long STALE_AFTER_MS = 15_000;
    private static final long GOODBYE_MS = 400;

    private final ServerLink link;
    private final Pinger pinger;

    private Session(ServerLink link, Pinger pinger) {
        this.link = link;
        this.pinger = pinger;
    }

    /**
     * Connects and says Hello (as a client that can reconnect). Blocking: call it from a background thread, never the
     * FX thread.
     *
     * @throws IllegalStateException if the server refuses the nickname
     */
    public static Session connect(String host, int port, String nickname, ClientState state) throws Exception {
        return open(host, port, state, new ClientMessage.Hello(UUID.randomUUID(), nickname, true));
    }

    /**
     * Connects again with the token from an earlier Welcome, to get the same identity back. Blocking, like
     * {@link #connect}.
     *
     * @throws SessionExpiredException if the server no longer knows the token (retrying is pointless)
     */
    public static Session reconnect(String host, int port, String token, ClientState state) throws Exception {
        return open(host, port, state, new ClientMessage.Reconnect(UUID.randomUUID(), token));
    }

    private static Session open(String host, int port, ClientState state, ClientMessage.Request handshake) throws Exception {
        ServerLink link = ServerLink.connect(host, port);
        Pinger pinger = new Pinger(() -> link.send(new ClientMessage.Ping()),
                ms -> Platform.runLater(() -> state.latencyProperty().set(ms)));
        try {
            link.addListener(message -> {
                if (message instanceof ServerMessage.Pong) {
                    pinger.pongReceived();
                }
                Platform.runLater(() -> state.apply(message));
            });
            // the first Ping goes at once: its Pong calibrates the countdown clock
            pinger.start(HEARTBEAT_MS, STALE_AFTER_MS, link::close);
            Response answer = link.request(handshake).get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!answer.isOk()) {
                throw answer.errorCode() == ErrorCode.UNKNOWN_TOKEN
                        ? new SessionExpiredException(describe(answer)) : new IllegalStateException(describe(answer));
            }
            link.whenClosed().thenRun(() -> Platform.runLater(
                    () -> state.connectionProperty().set(ClientState.Connection.DISCONNECTED)));
            Platform.runLater(() -> state.connectionProperty().set(ClientState.Connection.CONNECTED));
            return new Session(link, pinger);
        } catch (Exception e) {
            pinger.close();
            link.close();
            throw e;
        }
    }

    /** Sends a request; {@code onResult} runs on the FX thread with the answer (or a synthetic error if none came). */
    public void request(ClientMessage.Request request, Consumer<Response> onResult) {
        link.request(request).whenComplete((response, error) -> Platform.runLater(() -> onResult.accept(
                error == null ? response
                        : Response.error(request.requestId(), ErrorCode.INTERNAL_ERROR, "No answer from the server"))));
    }

    /** Sends a request and ignores the answer, apart from reporting a refusal to {@code onError}. */
    public void request(ClientMessage.Request request, Runnable onOk, Consumer<String> onError) {
        request(request, response -> {
            if (response.isOk()) {
                onOk.run();
            } else {
                onError.accept(describe(response));
            }
        });
    }

    /** A short human message for a refused request. */
    public static String describe(Response response) {
        return response.message() != null && !response.message().isBlank()
                ? response.message() : String.valueOf(response.errorCode());
    }

    /** Signs out politely (the server frees the nickname at once) and closes; waits a moment for the answer. */
    @Override
    public void close() {
        try {
            link.request(new ClientMessage.SignOut(UUID.randomUUID())).get(GOODBYE_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // the connection is already gone: nothing to say goodbye to
        }
        drop();
    }

    /** Signs out without waiting; the answer, or a short timeout, closes the connection. */
    public void signOut() {
        link.request(new ClientMessage.SignOut(UUID.randomUUID())).orTimeout(GOODBYE_MS, TimeUnit.MILLISECONDS)
                .whenComplete((answer, error) -> drop());
    }

    /** Closes abruptly, the way a network failure would: the server keeps the identity for a while so we can reconnect. */
    public void drop() {
        pinger.close();
        link.close();
    }
}
