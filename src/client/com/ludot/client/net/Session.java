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

    private final ServerLink link;
    private final Pinger pinger;

    private Session(ServerLink link, Pinger pinger) {
        this.link = link;
        this.pinger = pinger;
    }

    /**
     * Connects and says Hello. Blocking: call it from a background thread, never the FX thread.
     *
     * @throws IllegalStateException if the server refuses the nickname
     */
    public static Session connect(String host, int port, String nickname, ClientState state) throws Exception {
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
            pinger.start(HEARTBEAT_MS, link::close); // the first Ping goes at once: its Pong calibrates the countdown clock
            Response hello = link.request(new ClientMessage.Hello(UUID.randomUUID(), nickname))
                    .get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!hello.isOk()) {
                throw new IllegalStateException(hello.message());
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

    @Override
    public void close() {
        pinger.close();
        link.close();
    }
}
