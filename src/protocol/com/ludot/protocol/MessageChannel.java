package com.ludot.protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

/**
 * One end of a client-server connection: sends messages of type S and receives messages of type R.
 *
 * <p>Each message travels as a length-prefixed frame holding the serialised object. A fresh object
 * stream is used per message, so there is no stream-header handshake to deadlock, no back-reference
 * cache to grow or replay stale objects, and the size cap applies to every message individually.
 * Incoming objects are decoded under an allow-list filter, which blocks deserialisation attacks.
 *
 * <p>{@link #send} may be called from several threads; {@link #receive} is meant for one reader thread.
 */
public final class MessageChannel<S extends Serializable, R extends Serializable> implements Closeable {

    /** Largest frame accepted or produced, in bytes. */
    public static final int MAX_MESSAGE_BYTES = 1 << 20;

    // Only protocol types, collections, boxed primitives, strings and java.time values may be deserialised.
    private static final ObjectInputFilter ALLOW_LIST = ObjectInputFilter.Config.createFilter(
            "com.ludot.protocol.*;java.util.*;java.lang.*;java.time.*;"
                    + "maxdepth=20;maxrefs=20000;maxarray=100000;!*");

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Class<R> receiveType;
    private final Object writeLock = new Object();

    private MessageChannel(Socket socket, Class<R> receiveType) throws IOException {
        this.socket = socket;
        this.receiveType = receiveType;
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    /** The client's end: sends ClientMessages, receives ServerMessages. */
    public static MessageChannel<ClientMessage, ServerMessage> forClient(Socket socket) throws IOException {
        return new MessageChannel<>(socket, ServerMessage.class);
    }

    /** The server's end: sends ServerMessages, receives ClientMessages. */
    public static MessageChannel<ServerMessage, ClientMessage> forServer(Socket socket) throws IOException {
        return new MessageChannel<>(socket, ClientMessage.class);
    }

    /** Serialises and sends one message. Throws ProtocolException if it would exceed the size cap. */
    public void send(S message) throws IOException {
        byte[] frame = encode(message);
        synchronized (writeLock) {
            out.writeInt(frame.length);
            out.write(frame);
            out.flush();
        }
    }

    /**
     * Blocks until a whole message arrives.
     *
     * @throws java.io.EOFException if the peer closed the connection
     * @throws ProtocolException    if the frame is oversized, uses a disallowed class or is the wrong type
     */
    public R receive() throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_MESSAGE_BYTES) {
            throw new ProtocolException("Illegal frame length: " + length);
        }
        byte[] frame = new byte[length];
        in.readFully(frame);
        return decode(frame);
    }

    /** The remote address, for logging. */
    public String remoteAddress() {
        return String.valueOf(socket.getRemoteSocketAddress());
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private static byte[] encode(Serializable message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objects = new ObjectOutputStream(bytes)) {
            objects.writeObject(message);
        }
        if (bytes.size() > MAX_MESSAGE_BYTES) {
            throw new ProtocolException("Message too large: " + bytes.size() + " bytes");
        }
        return bytes.toByteArray();
    }

    private R decode(byte[] frame) throws IOException {
        try (ObjectInputStream objects = new ObjectInputStream(new ByteArrayInputStream(frame))) {
            objects.setObjectInputFilter(ALLOW_LIST);
            Object decoded = objects.readObject();
            if (!receiveType.isInstance(decoded)) {
                throw new ProtocolException("Unexpected message type: " + decoded.getClass().getName());
            }
            return receiveType.cast(decoded);
        } catch (ClassNotFoundException | java.io.InvalidClassException | java.io.StreamCorruptedException e) {
            throw new ProtocolException("Rejected message: " + e.getMessage(), e);
        } catch (java.io.OptionalDataException | NullPointerException | IllegalArgumentException e) {
            throw new ProtocolException("Malformed message: " + e.getMessage(), e);
        }
    }
}
