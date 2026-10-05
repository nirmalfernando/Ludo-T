package com.ludot.server.app;

import com.ludot.protocol.ServerMessage;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** A fake client connection: remembers everything offered to it, optionally with a bounded queue that never drains. */
class RecordingSink implements ClientSink {

    private final List<ServerMessage> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger capacity;
    private volatile boolean closed;

    RecordingSink() {
        this(Integer.MAX_VALUE);
    }

    RecordingSink(int capacity) {
        this.capacity = new AtomicInteger(capacity);
    }

    @Override
    public boolean offer(ServerMessage message) {
        if (capacity.getAndDecrement() <= 0) {
            return false;
        }
        received.add(message);
        return true;
    }

    @Override
    public void close() {
        closed = true;
    }

    boolean isClosed() {
        return closed;
    }

    List<ServerMessage> messages() {
        return List.copyOf(received);
    }

    <T extends ServerMessage> List<T> of(Class<T> type) {
        return received.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** Waits for a message of the type matching the predicate, searching from the start. */
    <T extends ServerMessage> T await(Class<T> type, Predicate<T> match, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (ServerMessage message : received) {
                if (type.isInstance(message) && match.test(type.cast(message))) {
                    return type.cast(message);
                }
            }
            pause();
        }
        throw new AssertionError("Timed out waiting for " + type.getSimpleName() + "; got " + received.size()
                + " messages, last: " + (received.isEmpty() ? "none" : received.get(received.size() - 1)));
    }

    <T extends ServerMessage> T await(Class<T> type, long timeoutMs) {
        return await(type, m -> true, timeoutMs);
    }

    ServerMessage.Response responseTo(UUID requestId) {
        return await(ServerMessage.Response.class, r -> r.requestId().equals(requestId), 5000);
    }

    private static void pause() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
