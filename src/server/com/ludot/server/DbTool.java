package com.ludot.server;

import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.JdbcBatchWriter;
import com.ludot.server.adapters.jdbc.RoomNumbers;
import com.ludot.server.adapters.jdbc.SqlScripts;
import com.ludot.server.adapters.persistence.AsyncGameRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Sets up and inspects the database: {@code DbTool [--config file] reset|schema|seed|demo|drop|status|verify [room]}.
 * It runs the .sql files in db/ over JDBC, so it needs neither psql nor the Supabase web console.
 * "reset" restores the initial state in one go: drop, create, seed, then play the demo game.
 */
public final class DbTool {

    private static final Path SQL_DIR = Path.of("db");
    private static final long DEMO_SEED = 2069L;
    private static final List<String> TABLES = List.of("players", "rooms", "seats", "decisions", "chat_messages");

    private DbTool() {
    }

    public static void main(String[] args) {
        try {
            String configFile = "config/server.properties";
            String command = null;
            String argument = null;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--config") && i + 1 < args.length) {
                    configFile = args[++i];
                } else if (command == null) {
                    command = args[i];
                } else {
                    argument = args[i];
                }
            }
            if (command == null) {
                usage();
                return;
            }
            ServerConfig config = ServerConfig.load(Path.of(configFile));
            if (!config.database().enabled()) {
                System.err.println("No database configured in " + configFile + " (set db.url, db.user and db.password).");
                System.exit(2);
            }
            boolean ok = command.equals("verify") ? verify(new Database(config.database()), argument)
                    : run(command, new Database(config.database()));
            if (!ok) {
                System.exit(3);
            }
        } catch (Exception e) {
            System.err.println("DbTool failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /** @return false only when a check (verify) failed */
    static boolean run(String command, Database database) throws Exception {
        switch (command) {
            case "drop" -> script(database, "reset.sql");
            case "schema" -> script(database, "schema.sql");
            case "seed" -> script(database, "seed.sql");
            case "demo" -> playDemo(database);
            case "status" -> status(database);
            case "reset" -> {
                script(database, "reset.sql");
                script(database, "schema.sql");
                script(database, "seed.sql");
                playDemo(database);
                status(database);
            }
            default -> usage();
        }
        return true;
    }

    private static void script(Database database, String name) throws IOException, SQLException {
        try (Connection connection = database.open()) {
            int statements = SqlScripts.run(connection, SQL_DIR.resolve(name));
            System.out.println(name + ": " + statements + " statements run");
        }
    }

    /** Plays the demo game through the real room and the asynchronous writer, then waits for it to be stored. */
    static void playDemo(Database database) throws Exception {
        long previous;
        try (Connection connection = database.open()) {
            previous = RoomNumbers.highest(connection);
        }
        String roomId = String.format("R-%04d", previous + 1);
        System.out.println("Playing the demo game as " + roomId + " (seed " + DEMO_SEED + ")...");
        try (JdbcBatchWriter writer = new JdbcBatchWriter(database);
             AsyncGameRepository repository = new AsyncGameRepository(writer)) {
            DemoGameSeeder.play(repository, roomId, DEMO_SEED);
            repository.awaitDrained(60_000);
            AsyncGameRepository.Stats stats = repository.stats();
            System.out.println("Stored " + stats.written() + " operations in " + stats.batches() + " batches"
                    + (stats.dropped() > 0 ? " (" + stats.dropped() + " could not be stored!)" : ""));
        }
    }

    /** Prints how long a round trip to the database takes (evidence for why writes are asynchronous) and the row counts. */
    static void status(Database database) throws SQLException {
        long start = System.nanoTime();
        try (Connection connection = database.open()) {
            long connectMs = (System.nanoTime() - start) / 1_000_000;
            long total = 0;
            int trips = 5;
            for (int i = 0; i < trips; i++) {
                long t = System.nanoTime();
                try (Statement statement = connection.createStatement(); ResultSet r = statement.executeQuery("SELECT 1")) {
                    r.next();
                }
                total += System.nanoTime() - t;
            }
            System.out.println("Connected in " + connectMs + " ms; a round trip takes about "
                    + total / trips / 1_000_000.0 + " ms");
            for (String table : TABLES) {
                try (Statement statement = connection.createStatement();
                     ResultSet r = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    r.next();
                    System.out.printf("  %-14s %d rows%n", table, r.getLong(1));
                } catch (SQLException e) {
                    System.out.printf("  %-14s (missing: run schema)%n", table);
                }
            }
        }
    }

    /**
     * Checks that the database holds exactly what a finished game played: the room is FINISHED with a winner,
     * and it has one stored step for every step the game took, numbered 0 to N-1 with no gap. With no room id the
     * most recent finished room is checked.
     */
    static boolean verify(Database database, String roomId) throws SQLException {
        try (Connection connection = database.open(); Statement statement = connection.createStatement()) {
            String id = roomId;
            if (id == null) {
                try (ResultSet r = statement.executeQuery("SELECT id FROM rooms WHERE status = 'FINISHED' ORDER BY finished_at DESC, id DESC")) {
                    id = r.next() ? r.getString(1) : null;
                }
            }
            if (id == null) {
                System.out.println("VERIFY FAIL: no finished room found");
                return false;
            }
            String status;
            String winner;
            long totalTurns;
            try (ResultSet r = statement.executeQuery("SELECT status, winner_colour, total_turns FROM rooms WHERE id = '" + id + "'")) {
                if (!r.next()) {
                    System.out.println("VERIFY FAIL: room " + id + " is not stored");
                    return false;
                }
                status = r.getString(1);
                winner = r.getString(2);
                totalTurns = r.getLong(3);
            }
            long steps;
            long min;
            long max;
            try (ResultSet r = statement.executeQuery("SELECT COUNT(*), MIN(turn_number), MAX(turn_number) FROM decisions WHERE room_id = '" + id + "'")) {
                r.next();
                steps = r.getLong(1);
                min = r.getLong(2);
                max = r.getLong(3);
            }
            long chat = scalar(statement, "SELECT COUNT(*) FROM chat_messages WHERE room_id = '" + id + "'");
            boolean ok = "FINISHED".equals(status) && winner != null && steps == totalTurns && min == 0 && max == totalTurns - 1;
            System.out.println("room " + id + ": status=" + status + " winner=" + winner + " game steps=" + totalTurns
                    + " stored steps=" + steps + " (numbered " + min + ".." + max + ") chat lines=" + chat);
            System.out.println(ok ? "VERIFY PASS: the database holds exactly what the game played"
                    : "VERIFY FAIL: stored steps do not match the game");
            return ok;
        }
    }

    private static long scalar(Statement statement, String sql) throws SQLException {
        try (ResultSet r = statement.executeQuery(sql)) {
            r.next();
            return r.getLong(1);
        }
    }

    private static void usage() {
        System.out.println("Usage: DbTool [--config file] reset | schema | seed | demo | drop | status | verify [room-id]");
        System.out.println("  reset   drop, create, seed and play the demo game (the demo's initial state)");
        System.out.println("  schema  create the tables if missing      seed  insert the demo people");
        System.out.println("  demo    play and store one finished game  drop  remove all tables");
        System.out.println("  status  show round-trip time and row counts");
        System.out.println("  verify  check a finished game is stored completely (default: the latest)");
    }
}
