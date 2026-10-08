package com.ludot.server;

import com.ludot.server.adapters.jdbc.DatabaseConfig;
import com.ludot.server.app.SessionConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ServerConfig(int port, int outboundCapacity, int readTimeoutMs, long queueLogIntervalMs,
                           SessionConfig session, DatabaseConfig database, int maxInFlight,
                           long reconnectGraceMs) {

    public static final int DEFAULT_MAX_IN_FLIGHT = 256;

    public static final long DEFAULT_RECONNECT_GRACE_MS = 30_000;

    public static ServerConfig defaults() {
        return new ServerConfig(5555, 1_000, 15_000, 1_000, SessionConfig.defaults(), DatabaseConfig.none(),
                DEFAULT_MAX_IN_FLIGHT, DEFAULT_RECONNECT_GRACE_MS);
    }

    public ServerConfig withPort(int newPort) {
        return new ServerConfig(newPort, outboundCapacity, readTimeoutMs, queueLogIntervalMs, session, database, maxInFlight, reconnectGraceMs);
    }

    public ServerConfig withOutboundCapacity(int capacity) {
        return new ServerConfig(port, capacity, readTimeoutMs, queueLogIntervalMs, session, database, maxInFlight, reconnectGraceMs);
    }

    public ServerConfig withReadTimeoutMs(int millis) {
        return new ServerConfig(port, outboundCapacity, millis, queueLogIntervalMs, session, database, maxInFlight, reconnectGraceMs);
    }

    public ServerConfig withSession(SessionConfig newSession) {
        return new ServerConfig(port, outboundCapacity, readTimeoutMs, queueLogIntervalMs, newSession, database, maxInFlight, reconnectGraceMs);
    }

    public ServerConfig withDatabase(DatabaseConfig newDatabase) {
        return new ServerConfig(port, outboundCapacity, readTimeoutMs, queueLogIntervalMs, session, newDatabase, maxInFlight, reconnectGraceMs);
    }

    public ServerConfig withMaxInFlight(int limit) {
        return new ServerConfig(port, outboundCapacity, readTimeoutMs, queueLogIntervalMs, session, database, limit, reconnectGraceMs);
    }

    public ServerConfig withReconnectGraceMs(long millis) {
        return new ServerConfig(port, outboundCapacity, readTimeoutMs, queueLogIntervalMs, session, database, maxInFlight, millis);
    }

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
                s.recentLogSize(),
                intOf(props, "room.requestMemory", s.requestMemory()),
                longOf(props, "turn.timeoutSeconds", s.turnTimeoutMs() / 1_000) * 1_000,
                intOf(props, "afk.timeoutsBeforeAfk", s.afkAfterTimeouts()));
        return new ServerConfig(intOf(props, "server.port", d.port()),
                intOf(props, "connection.outboundCapacity", d.outboundCapacity()),
                intOf(props, "connection.readTimeoutMs", d.readTimeoutMs()),
                longOf(props, "log.queueIntervalMs", d.queueLogIntervalMs()), session, database(props),
                intOf(props, "connection.maxInFlight", d.maxInFlight()),
                longOf(props, "connection.reconnectGraceMs", d.reconnectGraceMs()));
    }

    // the database is used when db.url is given, unless db.mode says "none"
    private static DatabaseConfig database(Properties props) {
        String url = props.getProperty("db.url", "").trim();
        String mode = props.getProperty("db.mode", url.isEmpty() ? "none" : "postgres").trim();
        return new DatabaseConfig(mode, url, props.getProperty("db.user", "").trim(), props.getProperty("db.password", ""));
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
