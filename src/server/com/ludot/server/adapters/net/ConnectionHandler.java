package com.ludot.server.adapters.net;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.MessageChannel;
import com.ludot.protocol.ProtocolException;
import com.ludot.protocol.ServerMessage;
import com.ludot.server.app.ClientSession;
import com.ludot.server.app.ClientSink;
import com.ludot.server.app.RequestRouter;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One connected client. A reader thread blocks on the socket and hands each message to the router; a
 * writer thread drains a bounded outbound queue to the socket. Everything else offers messages with
 * {@link #offer}, which never blocks: if the queue is full the client is too slow and is disconnected,
 * so one stalled client can never hold up a room or another client.
 *
 * <p>The socket has a read timeout; clients ping regularly, so silence means the connection is dead.
 */
final class ConnectionHandler implements ClientSink {

    private static final Logger LOG = Logger.getLogger(ConnectionHandler.class.getName());
    private static final long FLUSH_BEFORE_CLOSE_MS = 200;

    private final Socket socket;
    private final MessageChannel<ServerMessage, ClientMessage> channel;
    private final BlockingQueue<ServerMessage> outbound;
    private final RequestRouter router;
    private final ClientSession session = new ClientSession(this);
    private final Runnable onClosed;
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile Thread writerThread;

    ConnectionHandler(Socket socket, RequestRouter router, int outboundCapacity, int readTimeoutMs,
                      Runnable onClosed) throws IOException {
        this.socket = socket;
        this.router = router;
        this.onClosed = onClosed;
        this.outbound = new ArrayBlockingQueue<>(outboundCapacity);
        socket.setSoTimeout(readTimeoutMs);
        socket.setTcpNoDelay(true);
        this.channel = MessageChannel.forServer(socket);
    }

    String remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean offer(ServerMessage message) {
        return !closed.get() && outbound.offer(message);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Error closing socket", e);
        }
        Thread writer = writerThread;
        if (writer != null) {
            writer.interrupt();
        }
        router.disconnected(session);
        onClosed.run();
    }

    /** Reader thread body: read, route, repeat until the client goes away or breaks the protocol. */
    void readLoop() {
        boolean protocolViolation = false;
        try {
            while (!closed.get()) {
                ClientMessage message = channel.receive();
                if (!router.route(session, message)) {
                    protocolViolation = true;
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            LOG.info("No heartbeat from " + remoteAddress() + "; closing");
        } catch (EOFException e) {
            LOG.fine("Client closed " + remoteAddress());
        } catch (ProtocolException e) {
            protocolViolation = true;
            LOG.warning("Protocol error from " + remoteAddress() + ": " + e.getMessage());
        } catch (IOException e) {
            LOG.fine("Connection error from " + remoteAddress() + ": " + e.getMessage());
        } finally {
            if (protocolViolation) {
                flushBriefly();
            }
            close();
        }
    }

    /** Writer thread body: send queued messages in order until closed. */
    void writeLoop() {
        writerThread = Thread.currentThread();
        try {
            while (!closed.get()) {
                channel.send(outbound.take());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            LOG.fine("Write failed to " + remoteAddress() + ": " + e.getMessage());
        } finally {
            close();
        }
    }

    // give an error response a moment to reach the client before the socket is closed
    private void flushBriefly() {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(FLUSH_BEFORE_CLOSE_MS);
        while (!outbound.isEmpty() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
