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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private static final long WINDOW_WAIT_SECONDS = 5;

    private final Socket socket;
    private final MessageChannel<ServerMessage, ClientMessage> channel;
    private final BlockingQueue<ServerMessage> outbound;
    private final RequestRouter router;
    private final ClientSession session = new ClientSession(this);
    private final Runnable onClosed;
    private final Semaphore window;          // one permit per request that has not yet had its answer sent
    private final int maxInFlight;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger unsent = new AtomicInteger(); // queued or being written: not yet on the wire
    private volatile Thread writerThread;

    ConnectionHandler(Socket socket, RequestRouter router, int outboundCapacity, int readTimeoutMs,
                      int maxInFlight, Runnable onClosed) throws IOException {
        this.maxInFlight = maxInFlight;
        this.window = new Semaphore(maxInFlight);
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
        if (closed.get()) {
            return false;
        }
        unsent.incrementAndGet();
        if (outbound.offer(message)) {
            return true;
        }
        unsent.decrementAndGet();
        LOG.warning("Closing " + remoteAddress() + ": its outbound queue is full (" + outbound.size()
                + " messages waiting), so the client is too slow to keep up");
        return false;
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
                if (message instanceof ClientMessage.Request) {
                    awaitWindow();
                }
                if (!router.route(session, message)) {
                    protocolViolation = true;
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
                ServerMessage message = outbound.take();
                try {
                    channel.send(message);
                } finally {
                    unsent.decrementAndGet();
                }
                if (message instanceof ServerMessage.Response) {
                    releaseWindow(); // the answer is on the wire: the client may have another request in flight
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            LOG.fine("Write failed to " + remoteAddress() + ": " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Backpressure on a client that pipelines faster than it can take answers: at most maxInFlight requests may be
     * unanswered (or answered but not yet sent). Past that the reader stops reading, the socket buffer fills and
     * TCP slows the sender, instead of the answers piling up until the client is dropped as too slow. If answers
     * somehow stop (they should not), the wait is capped so the connection cannot stall for good.
     */
    private void awaitWindow() throws InterruptedException {
        if (!window.tryAcquire(WINDOW_WAIT_SECONDS, TimeUnit.SECONDS)) {
            LOG.warning(remoteAddress() + " has had no answers for " + WINDOW_WAIT_SECONDS + " s; reading on regardless");
        }
    }

    private void releaseWindow() {
        if (window.availablePermits() < maxInFlight) {
            window.release();
        }
    }

    // give an error response a moment to reach the client before the socket is closed
    private void flushBriefly() {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(FLUSH_BEFORE_CLOSE_MS);
        // not outbound.isEmpty(): the writer may already have taken the reply off the queue without having written it
        while (unsent.get() > 0 && System.nanoTime() < deadline) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
