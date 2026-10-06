package com.ludot.server.adapters.jdbc;

import com.ludot.protocol.Colour;
import com.ludot.protocol.FinishedGame;
import com.ludot.protocol.FinishedGameList;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.Variant;
import com.ludot.server.app.GameHistory;
import com.ludot.server.app.HistoryException;
import com.ludot.server.app.ReplayData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads finished games from the database. One connection is kept and reused (opening one to a hosted database is
 * slow), and reopened if it breaks. The SQL is plain and portable, so the same queries run on PostgreSQL and H2.
 */
public final class JdbcGameHistory implements GameHistory, AutoCloseable {

    private static final String FINISHED = "SELECT id, mode, variant, seed, winner_colour, finished_at, total_turns "
            + "FROM rooms WHERE status = 'FINISHED' AND winner_colour IS NOT NULL "
            + "ORDER BY finished_at DESC, id DESC LIMIT ? OFFSET ?";
    private static final String ROOM = "SELECT variant, seed, winner_colour FROM rooms "
            + "WHERE id = ? AND status = 'FINISHED' AND winner_colour IS NOT NULL";
    private static final String DECISIONS = "SELECT turn_number, colour, roll, option_index FROM decisions "
            + "WHERE room_id = ? ORDER BY turn_number";

    private final Database database;
    private Connection connection;

    public JdbcGameHistory(Database database) {
        this.database = database;
    }

    @Override
    public synchronized FinishedGameList finished(int page) {
        int number = Math.max(0, page);
        try (PreparedStatement statement = connection().prepareStatement(FINISHED)) {
            statement.setInt(1, PAGE_SIZE + 1); // one extra row tells whether there is another page
            statement.setInt(2, number * PAGE_SIZE);
            List<FinishedGame> games = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    games.add(game(rows));
                }
            }
            boolean more = games.size() > PAGE_SIZE;
            return new FinishedGameList(more ? games.subList(0, PAGE_SIZE) : games, number, more);
        } catch (SQLException e) {
            throw failed(e);
        }
    }

    @Override
    public synchronized Optional<ReplayData> load(String gameId) {
        try (PreparedStatement room = connection().prepareStatement(ROOM)) {
            room.setString(1, gameId);
            try (ResultSet row = room.executeQuery()) {
                if (!row.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ReplayData(gameId, Variant.valueOf(row.getString(1)), row.getLong(2),
                        Colour.valueOf(row.getString(3)), steps(gameId)));
            }
        } catch (SQLException e) {
            throw failed(e);
        }
    }

    private List<ReplayData.Step> steps(String gameId) throws SQLException {
        List<ReplayData.Step> steps = new ArrayList<>();
        try (PreparedStatement statement = connection().prepareStatement(DECISIONS)) {
            statement.setString(1, gameId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    steps.add(new ReplayData.Step(rows.getInt(1), Colour.valueOf(rows.getString(2)),
                            nullableInt(rows, 3), nullableInt(rows, 4)));
                }
            }
        }
        return steps;
    }

    private static FinishedGame game(ResultSet row) throws SQLException {
        Timestamp finished = row.getTimestamp(6);
        return new FinishedGame(row.getString(1), RoomMode.valueOf(row.getString(2)), Variant.valueOf(row.getString(3)),
                row.getLong(4), Colour.valueOf(row.getString(5)), finished == null ? Instant.EPOCH : finished.toInstant(),
                row.getInt(7));
    }

    private static Integer nullableInt(ResultSet row, int column) throws SQLException {
        int value = row.getInt(column);
        return row.wasNull() ? null : value;
    }

    private Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = database.open();
        }
        return connection;
    }

    // a broken connection is dropped so the next call opens a fresh one
    private HistoryException failed(SQLException cause) {
        close();
        return new HistoryException("Could not read the stored games: " + cause.getMessage(), cause);
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // already unusable
            }
            connection = null;
        }
    }
}
