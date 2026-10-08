package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.MessageChannel;
import com.ludot.protocol.Phase;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

/** A real TCP client for integration tests: a reader thread records everything the server sends. */
final class TestClient implements AutoCloseable {

    final RecordingSink inbox = new RecordingSink();
    private final Socket socket;
    private final MessageChannel<ClientMessage, ServerMessage> channel;
    private volatile boolean disconnected;

    private TestClient(Socket socket) throws IOException {
        this.socket = socket;
        this.channel = MessageChannel.forClient(socket);
        Thread reader = new Thread(this::readLoop, "test-client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    static TestClient connect(int port) throws IOException {
        return new TestClient(new Socket(InetAddress.getLoopbackAddress(), port));
    }

    private void readLoop() {
        try {
            while (true) {
                inbox.offer(channel.receive());
            }
        } catch (IOException e) {
            disconnected = true;
        }
    }

    void send(ClientMessage message) throws IOException {
        channel.send(message);
    }

    Response call(ClientMessage.Request request) throws IOException {
        send(request);
        return inbox.responseTo(request.requestId());
    }

    TestClient hello(String nickname) throws IOException {
        Response response = call(new ClientMessage.Hello(UUID.randomUUID(), nickname));
        if (!response.isOk()) {
            throw new AssertionError("Hello failed: " + response);
        }
        return this;
    }

    void sendRaw(byte[] bytes) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.write(bytes);
        out.flush();
    }

    boolean awaitDisconnected(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (disconnected) {
                return true;
            }
            Kit.sleep(5);
        }
        return disconnected;
    }

    /** Answers every YourTurn by rolling, or by choosing the first move, until GameOver arrives. */
    boolean playUntilGameOver(String roomId, long timeoutMs) throws IOException {
        int cursor = 0;
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            java.util.List<ServerMessage> all = inbox.messages();
            for (; cursor < all.size(); cursor++) {
                ServerMessage message = all.get(cursor);
                if (message instanceof ServerMessage.GameOver) {
                    return true;
                }
                if (message instanceof ServerMessage.YourTurn turn) {
                    send(turn.phase() != Phase.AWAITING_MOVE
                            ? new ClientMessage.Roll(UUID.randomUUID(), roomId, turn.turnNumber())
                            : new ClientMessage.ChooseMove(UUID.randomUUID(), roomId, turn.turnNumber(), 0));
                }
            }
            Kit.sleep(1);
        }
        return false;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // already closed
        }
    }
}
