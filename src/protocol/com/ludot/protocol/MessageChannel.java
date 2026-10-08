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

public final class MessageChannel<S extends Serializable, R extends Serializable> implements Closeable {

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

    public static MessageChannel<ClientMessage, ServerMessage> forClient(Socket socket) throws IOException {
        return new MessageChannel<>(socket, ServerMessage.class);
    }

    public static MessageChannel<ServerMessage, ClientMessage> forServer(Socket socket) throws IOException {
        return new MessageChannel<>(socket, ClientMessage.class);
    }

    public void send(S message) throws IOException {
        byte[] frame = encode(message);
        synchronized (writeLock) {
            out.writeInt(frame.length);
            out.write(frame);
            out.flush();
        }
    }

    public R receive() throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_MESSAGE_BYTES) {
            throw new ProtocolException("Illegal frame length: " + length);
        }
        byte[] frame = new byte[length];
        in.readFully(frame);
        return decode(frame);
    }

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
