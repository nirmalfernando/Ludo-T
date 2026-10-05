package com.ludot.protocol;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageChannel")
class MessageChannelTest {

    private Socket clientSocket;
    private Socket serverSocket;
    private MessageChannel<ClientMessage, ServerMessage> client;
    private MessageChannel<ServerMessage, ClientMessage> server;

    @BeforeEach void connect() throws IOException {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            clientSocket = new Socket(InetAddress.getLoopbackAddress(), listener.getLocalPort());
            serverSocket = listener.accept();
        }
        client = MessageChannel.forClient(clientSocket);
        server = MessageChannel.forServer(serverSocket);
        clientSocket.setSoTimeout(5000);
        serverSocket.setSoTimeout(5000);
    }

    @AfterEach void disconnect() throws IOException {
        clientSocket.close();
        serverSocket.close();
    }

    // writes a raw frame, bypassing MessageChannel, to simulate a misbehaving peer
    private void writeRawFrame(Socket socket, byte[] payload) throws IOException {
        DataOutputStream raw = new DataOutputStream(socket.getOutputStream());
        raw.writeInt(payload.length);
        raw.write(payload);
        raw.flush();
    }

    private static byte[] serialise(Object value) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (ObjectOutputStream objects = new ObjectOutputStream(bytes)) {
            objects.writeObject(value);
        }
        return bytes.toByteArray();
    }

    @Test @DisplayName("every client message type survives a round trip")
    void clientMessagesRoundTrip() throws IOException {
        for (ClientMessage message : Samples.clientMessages()) {
            client.send(message);
            assertEquals(message, server.receive());
        }
    }

    @Test @DisplayName("every server message type survives a round trip")
    void serverMessagesRoundTrip() throws IOException {
        for (ServerMessage message : Samples.serverMessages()) {
            server.send(message);
            assertEquals(message, client.receive());
        }
    }

    @Test @DisplayName("the samples cover every concrete message type, so new messages cannot skip the round trip")
    void samplesCoverEveryMessageType() {
        Set<Class<?>> sampled = new HashSet<>();
        Samples.clientMessages().forEach(m -> sampled.add(m.getClass()));
        Samples.serverMessages().forEach(m -> sampled.add(m.getClass()));

        Set<Class<?>> concrete = new HashSet<>();
        collectRecords(ClientMessage.class, concrete);
        collectRecords(ServerMessage.class, concrete);

        assertEquals(concrete, sampled);
    }

    private static void collectRecords(Class<?> sealedType, Set<Class<?>> into) {
        for (Class<?> permitted : sealedType.getPermittedSubclasses()) {
            if (permitted.isRecord()) {
                into.add(permitted);
            } else {
                collectRecords(permitted, into);
            }
        }
    }

    @Test @DisplayName("many threads can send on one channel without interleaving frames")
    void concurrentSendsStayIntact() throws Exception {
        int threads = 8;
        int perThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            int id = t;
            pool.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    client.send(new ClientMessage.Roll(Samples.ID, "R-" + id, i));
                }
                return null;
            });
        }
        int[] seen = new int[threads];
        for (int i = 0; i < threads * perThread; i++) {
            ClientMessage.Roll roll = (ClientMessage.Roll) server.receive();
            int id = Integer.parseInt(roll.roomId().substring(2));
            assertEquals(seen[id]++, roll.turnNumber(), "per-thread order must be preserved");
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test @DisplayName("an oversized frame length is rejected before any allocation")
    void oversizedFrameRejected() throws IOException {
        DataOutputStream raw = new DataOutputStream(clientSocket.getOutputStream());
        raw.writeInt(MessageChannel.MAX_MESSAGE_BYTES + 1);
        raw.flush();
        assertThrows(ProtocolException.class, server::receive);
    }

    @Test @DisplayName("a zero or negative frame length is rejected")
    void nonPositiveFrameRejected() throws IOException {
        DataOutputStream raw = new DataOutputStream(clientSocket.getOutputStream());
        raw.writeInt(-5);
        raw.flush();
        assertThrows(ProtocolException.class, server::receive);
    }

    @Test @DisplayName("a message that serialises above the cap is refused on send and the channel stays usable")
    void oversizedMessageRefusedOnSend() throws IOException {
        List<RoomSummary> rooms = new ArrayList<>();
        for (int i = 0; i < 40_000; i++) {
            rooms.add(new RoomSummary("R-" + i, RoomMode.PLAY, Variant.LUDO_T, RoomStatus.WAITING,
                    1, 3, 0, "host-nickname-" + i));
        }
        assertThrows(ProtocolException.class, () -> server.send(new ServerMessage.LobbyUpdate(1, rooms)));

        server.send(new ServerMessage.Pong(1));
        assertEquals(new ServerMessage.Pong(1), client.receive());
    }

    @Test @DisplayName("a class outside the allow-list is rejected during deserialisation")
    void disallowedClassRejected() throws IOException {
        writeRawFrame(clientSocket, serialise(new File("/etc/passwd")));
        ProtocolException rejected = assertThrows(ProtocolException.class, server::receive);
        // the allow-list must stop it during decoding, not the later type check
        assertTrue(rejected.getMessage().contains("REJECTED"), rejected.getMessage());
    }

    @Test @DisplayName("a serialisable object that is not a message is rejected")
    void nonMessageRejected() throws IOException {
        writeRawFrame(clientSocket, serialise("just a string"));
        assertThrows(ProtocolException.class, server::receive);
    }

    @Test @DisplayName("a server message arriving at the server is the wrong direction and is rejected")
    void wrongDirectionRejected() throws IOException {
        writeRawFrame(clientSocket, serialise(new ServerMessage.Pong(1)));
        assertThrows(ProtocolException.class, server::receive);
    }

    @Test @DisplayName("garbage bytes are a protocol error, not a crash")
    void garbageRejected() throws IOException {
        writeRawFrame(clientSocket, new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        assertThrows(ProtocolException.class, server::receive);
    }

    @Test @DisplayName("a closed peer surfaces as EOFException")
    void closedPeerIsEof() throws IOException {
        clientSocket.close();
        assertThrows(EOFException.class, server::receive);
    }
}
