package com.ludot.server;

import com.ludot.server.app.SessionConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Everything configurable about a server process; defaults match the design document. */
public record ServerConfig(int port, int outboundCapacity, int readTimeoutMs, long queueLogIntervalMs,
                           SessionConfig session) {

    public static ServerConfig defaults() {
        return new ServerConfig(5555, 1_000, 15_000, 1_000, SessionConfig.defaults());
    }

    public ServerConfig withPort(int newPort) {
        return new ServerConfig(newPort, outboundCapacity, readTimeoutMs, queueLogIntervalMs, session);
    }

    public ServerConfig withOutboundCapacity(int capacity) {
        return new ServerConfig(port, capacity, readTimeoutMs, queueLogIntervalMs, session);
    }

    public ServerConfig withReadTimeoutMs(int millis) {
        return new ServerConfig(port, outboundCapacity, millis, queueLogIntervalMs, session);
    }

    public ServerConfig withSession(SessionConfig newSession) {
        return new ServerConfig(port, outboundCapacity, readTimeoutMs, queueLogIntervalMs, newSession);
    }

    /** Reads a properties file; a missing file or key falls back to the default. */
    public static ServerConfig load(Path file) throws IOException {
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                props.load(reader);
            }
        }
        ServerConfig d = defaults();
        SessionConfig s = d.session();
        SessionConfig session = new SessionConfig(
                intOf(props, "room.queueCapacity", s.queueCapacity()),
                longOf(props, "speed.fastMs", s.fastMs()),
                longOf(props, "speed.normalMs", s.normalMs()),
                longOf(props, "speed.slowMs", s.slowMs()),
                intOf(props, "chat.maxLength", s.chatMaxLength()),
                intOf(props, "chat.historySize", s.chatHistorySize()),
                intOf(props, "chat.rateLimitMessages", s.chatRateLimitMessages()),
                longOf(props, "chat.rateLimitWindowMs", s.chatRateLimitWindowMs()),
                longOf(props, "room.emptyCloseMs", s.emptyRoomCloseMs()),
                s.recentLogSize());
        return new ServerConfig(intOf(props, "server.port", d.port()),
                intOf(props, "connection.outboundCapacity", d.outboundCapacity()),
                intOf(props, "connection.readTimeoutMs", d.readTimeoutMs()),
                longOf(props, "log.queueIntervalMs", d.queueLogIntervalMs()), session);
    }

    private static int intOf(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Integer.parseInt(value.trim());
    }

    private static long longOf(Properties props, String key, long fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Long.parseLong(value.trim());
    }
}
