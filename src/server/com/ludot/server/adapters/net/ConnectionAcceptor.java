package com.ludot.server.adapters.net;

import com.ludot.server.app.RequestRouter;
import com.ludot.server.concurrent.NamedThreadFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConnectionAcceptor implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(ConnectionAcceptor.class.getName());

    private final int requestedPort;
    private final RequestRouter router;
    private final int outboundCapacity;
    private final int readTimeoutMs;
    private final int maxInFlight;
    private final ExecutorService readers = Executors.newCachedThreadPool(new NamedThreadFactory("conn-reader", true));
    private final ExecutorService writers = Executors.newCachedThreadPool(new NamedThreadFactory("conn-writer", true));
    private final Set<ConnectionHandler> connections = ConcurrentHashMap.newKeySet();
    private ServerSocket serverSocket;
    private Thread acceptorThread;

    public ConnectionAcceptor(int port, RequestRouter router, int outboundCapacity, int readTimeoutMs,
                              int maxInFlight) {
        this.maxInFlight = maxInFlight;
        this.requestedPort = port;
        this.router = router;
        this.outboundCapacity = outboundCapacity;
        this.readTimeoutMs = readTimeoutMs;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        acceptorThread = new NamedThreadFactory("acceptor", false).newThread(this::acceptLoop);
        acceptorThread.start();
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public int connectionCount() {
        return connections.size();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                handOff(serverSocket.accept());
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    LOG.log(Level.WARNING, "Accept failed", e);
                }
            }
        }
    }

    private void handOff(Socket socket) {
        try {
            ConnectionHandler[] holder = new ConnectionHandler[1];
            holder[0] = new ConnectionHandler(socket, router, outboundCapacity, readTimeoutMs, maxInFlight,
                    () -> connections.remove(holder[0]));
            connections.add(holder[0]);
            writers.execute(holder[0]::writeLoop);
            readers.execute(holder[0]::readLoop);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not set up connection", e);
            closeQuietly(socket);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Error closing socket", e);
        }
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Error closing server socket", e);
        }
        connections.forEach(ConnectionHandler::close);
        readers.shutdownNow();
        writers.shutdownNow();
    }
}
