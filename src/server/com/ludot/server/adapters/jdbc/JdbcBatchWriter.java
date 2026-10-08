package com.ludot.server.adapters.jdbc;

import com.ludot.server.app.BatchWriter;
import com.ludot.server.app.ChatRecord;
import com.ludot.server.app.DecisionRecord;
import com.ludot.server.app.PersistenceException;
import com.ludot.server.app.PlayerRecord;
import com.ludot.server.app.RoomRecord;
import com.ludot.server.app.RoomStatusRecord;
import com.ludot.server.app.SeatsRecord;
import com.ludot.server.app.WriteOp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// inserts that could be repeated after an unconfirmed commit are written "unless already there", which makes a retry harmless
public final class JdbcBatchWriter implements BatchWriter, AutoCloseable {

    private static final Logger LOG = Logger.getLogger(JdbcBatchWriter.class.getName());
    private static final int VALIDITY_CHECK_SECONDS = 2;

    // "insert unless already there" in standard SQL, so it runs unchanged on PostgreSQL and H2
    private static final Map<Class<? extends WriteOp>, String> SQL = Map.of(
            PlayerRecord.class, "INSERT INTO players (id, nickname) "
                    + "SELECT CAST(? AS VARCHAR), CAST(? AS VARCHAR) "
                    + "WHERE NOT EXISTS (SELECT 1 FROM players WHERE id = CAST(? AS VARCHAR))",
            RoomRecord.class, "INSERT INTO rooms (id, mode, variant, seed, status, host_id) "
                    + "SELECT CAST(? AS VARCHAR), CAST(? AS VARCHAR), CAST(? AS VARCHAR), CAST(? AS BIGINT), "
                    + "'WAITING', CAST(? AS VARCHAR) "
                    + "WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = CAST(? AS VARCHAR))",
            RoomStatusRecord.class, "UPDATE rooms SET status = ?, winner_colour = ?, total_turns = ?, finished_at = ? WHERE id = ?",
            DecisionRecord.class, "INSERT INTO decisions (room_id, turn_number, colour, roll, option_index, source) "
                    + "SELECT CAST(? AS VARCHAR), CAST(? AS INTEGER), CAST(? AS VARCHAR), CAST(? AS INTEGER), "
                    + "CAST(? AS INTEGER), CAST(? AS VARCHAR) "
                    + "WHERE NOT EXISTS (SELECT 1 FROM decisions WHERE room_id = CAST(? AS VARCHAR) "
                    + "AND turn_number = CAST(? AS INTEGER))",
            ChatRecord.class, "INSERT INTO chat_messages (room_id, channel_seq, player_id, text, is_system, sent_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)");
    private static final String DELETE_SEATS = "DELETE FROM seats WHERE room_id = ?";
    private static final String INSERT_SEAT = "INSERT INTO seats (room_id, colour, player_id, controller) VALUES (?, ?, ?, ?)";

    private interface Binder {
        void bind(PreparedStatement statement, WriteOp operation) throws SQLException;
    }

    private static final Map<Class<? extends WriteOp>, Binder> BINDERS = Map.of(
            PlayerRecord.class, (s, op) -> bindPlayer(s, (PlayerRecord) op),
            RoomRecord.class, (s, op) -> bindRoom(s, (RoomRecord) op),
            RoomStatusRecord.class, (s, op) -> bindStatus(s, (RoomStatusRecord) op),
            DecisionRecord.class, (s, op) -> bindDecision(s, (DecisionRecord) op),
            ChatRecord.class, (s, op) -> bindChat(s, (ChatRecord) op));

    private final Database database;
    private Connection connection;

    public JdbcBatchWriter(Database database) {
        this.database = database;
    }

    @Override
    public synchronized void writeAll(List<WriteOp> operations) throws PersistenceException {
        try {
            Connection conn = usableConnection();
            conn.setAutoCommit(false);
            try {
                apply(conn, operations);
                conn.commit();
            } catch (SQLException e) {
                rollbackAndForget(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not write " + operations.size() + " operations: " + e.getMessage(), e);
        }
    }

    private Connection usableConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(VALIDITY_CHECK_SECONDS)) {
            closeQuietly();
            connection = database.open();
        }
        return connection;
    }

    // after a failure the connection's state is unknown, so the next write opens a fresh one
    private void rollbackAndForget(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Rollback failed", e);
        }
        closeQuietly();
    }

    private void apply(Connection conn, List<WriteOp> operations) throws SQLException {
        Map<Class<?>, PreparedStatement> statements = new HashMap<>();
        Class<?> pending = null;
        try {
            for (WriteOp operation : operations) {
                if (operation instanceof SeatsRecord seats) {
                    flush(statements.get(pending));
                    pending = null;
                    writeSeats(conn, seats);
                    continue;
                }
                Class<?> type = operation.getClass();
                if (pending != null && pending != type) {
                    flush(statements.get(pending));
                }
                pending = type;
                PreparedStatement statement = statements.get(type);
                if (statement == null) {
                    statement = conn.prepareStatement(SQL.get(type));
                    statements.put(type, statement);
                }
                BINDERS.get(type).bind(statement, operation);
                statement.addBatch();
            }
            flush(statements.get(pending));
        } finally {
            for (PreparedStatement statement : statements.values()) {
                statement.close();
            }
        }
    }

    private static void flush(PreparedStatement statement) throws SQLException {
        if (statement != null) {
            statement.executeBatch();
        }
    }

    // the seats of a room are replaced as a set; delete then insert is portable across PostgreSQL and H2
    private static void writeSeats(Connection conn, SeatsRecord record) throws SQLException {
        try (PreparedStatement delete = conn.prepareStatement(DELETE_SEATS)) {
            delete.setString(1, record.roomId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = conn.prepareStatement(INSERT_SEAT)) {
            for (SeatsRecord.SeatRow seat : record.seats()) {
                insert.setString(1, record.roomId());
                insert.setString(2, seat.colour().name());
                setNullableString(insert, 3, seat.playerId());
                insert.setString(4, seat.controller().name());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void bindPlayer(PreparedStatement s, PlayerRecord p) throws SQLException {
        s.setString(1, p.id());
        s.setString(2, p.nickname());
        s.setString(3, p.id());
    }

    private static void bindRoom(PreparedStatement s, RoomRecord r) throws SQLException {
        s.setString(1, r.roomId());
        s.setString(2, r.mode().name());
        s.setString(3, r.variant().name());
        s.setLong(4, r.seed());
        setNullableString(s, 5, r.hostId());
        s.setString(6, r.roomId());
    }

    private static void bindStatus(PreparedStatement s, RoomStatusRecord r) throws SQLException {
        s.setString(1, r.status().name());
        setNullableString(s, 2, r.winner() == null ? null : r.winner().name());
        s.setInt(3, r.totalTurns());
        boolean ended = r.status() == com.ludot.protocol.RoomStatus.FINISHED || r.status() == com.ludot.protocol.RoomStatus.CLOSED;
        if (ended) {
            s.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        } else {
            s.setNull(4, Types.TIMESTAMP);
        }
        s.setString(5, r.roomId());
    }

    private static void bindDecision(PreparedStatement s, DecisionRecord d) throws SQLException {
        s.setString(1, d.roomId());
        s.setInt(2, d.turnNumber());
        s.setString(3, d.colour().name());
        setNullableInt(s, 4, d.roll());
        setNullableInt(s, 5, d.optionIndex());
        s.setString(6, d.source().name());
        s.setString(7, d.roomId());
        s.setInt(8, d.turnNumber());
    }

    private static void bindChat(PreparedStatement s, ChatRecord c) throws SQLException {
        setNullableString(s, 1, c.roomId());
        s.setLong(2, c.channelSeq());
        setNullableString(s, 3, c.playerId());
        s.setString(4, c.text());
        s.setBoolean(5, c.system());
        s.setTimestamp(6, new Timestamp(c.sentAt()));
    }

    private static void setNullableString(PreparedStatement s, int index, String value) throws SQLException {
        if (value == null) {
            s.setNull(index, Types.VARCHAR);
        } else {
            s.setString(index, value);
        }
    }

    private static void setNullableInt(PreparedStatement s, int index, Integer value) throws SQLException {
        if (value == null) {
            s.setNull(index, Types.INTEGER);
        } else {
            s.setInt(index, value);
        }
    }

    private void closeQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.log(Level.FINE, "Error closing connection", e);
            }
            connection = null;
        }
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }
}
