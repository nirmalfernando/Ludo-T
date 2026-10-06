package com.ludot.server.adapters;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.Variant;
import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.DatabaseConfig;
import com.ludot.server.adapters.jdbc.JdbcBatchWriter;
import com.ludot.server.adapters.jdbc.SqlScripts;
import com.ludot.server.app.ChatRecord;
import com.ludot.server.app.DecisionRecord;
import com.ludot.server.app.DecisionSource;
import com.ludot.server.app.PersistenceException;
import com.ludot.server.app.PlayerRecord;
import com.ludot.server.app.RoomRecord;
import com.ludot.server.app.RoomStatusRecord;
import com.ludot.server.app.SeatsRecord;
import com.ludot.server.app.WriteOp;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The SQL scripts and the batch writer, against an in-memory H2 database speaking PostgreSQL. */
@DisplayName("JdbcBatchWriter and the SQL scripts")
class JdbcBatchWriterTest {

    private static final String PLAYER = "p-1";

    private Database database;
    private JdbcBatchWriter writer;
    private Connection reader;

    @BeforeEach void setUp() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        database = new Database(new DatabaseConfig("h2", url, "sa", ""));
        reader = database.open();
        SqlScripts.run(reader, Path.of("db", "schema.sql"));
        writer = new JdbcBatchWriter(database);
    }

    @AfterEach void tearDown() throws Exception {
        writer.close();
        reader.close();
    }

    private long count(String table) throws SQLException {
        try (Statement s = reader.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next();
            return r.getLong(1);
        }
    }

    private String scalar(String sql) throws SQLException {
        try (Statement s = reader.createStatement(); ResultSet r = s.executeQuery(sql)) {
            return r.next() ? r.getString(1) : null;
        }
    }

    private static RoomRecord room(String id) {
        return new RoomRecord(id, RoomMode.BOT_MATCH, Variant.LUDO_T, 2069L, PLAYER);
    }

    private static DecisionRecord decision(String room, int turn, Integer option) {
        return new DecisionRecord(room, turn, Colour.RED, 4, option, DecisionSource.AI);
    }

    // ---------------------------------------------------------------- scripts

    @Test @DisplayName("statements are split on semicolons, comments and blank lines are dropped")
    void splitting() {
        List<String> statements = SqlScripts.split("-- header\nCREATE TABLE a (x INT); -- trailing\n\nDROP TABLE a;\n;\n");
        assertEquals(List.of("CREATE TABLE a (x INT)", "DROP TABLE a"), statements);
    }

    @Test @DisplayName("the schema can be applied twice and reset removes every table")
    void schemaIdempotentAndReset() throws Exception {
        SqlScripts.run(reader, Path.of("db", "schema.sql"));
        assertEquals(0, count("rooms"));
        SqlScripts.run(reader, Path.of("db", "reset.sql"));
        assertThrows(SQLException.class, () -> count("rooms"));
        SqlScripts.run(reader, Path.of("db", "schema.sql"));
        SqlScripts.run(reader, Path.of("db", "seed.sql"));
        assertEquals(2, count("players"));
    }

    // ---------------------------------------------------------------- writes

    @Test @DisplayName("every kind of operation is stored with the values given")
    void everyOperationIsStored() throws Exception {
        writer.writeAll(List.of(
                new PlayerRecord(PLAYER, "alice"),
                room("R-0001"),
                new SeatsRecord("R-0001", List.of(
                        new SeatsRecord.SeatRow(Colour.RED, PLAYER, Controller.HUMAN),
                        new SeatsRecord.SeatRow(Colour.GREEN, null, Controller.AI))),
                decision("R-0001", 0, null),
                decision("R-0001", 1, 2),
                new ChatRecord("R-0001", 7, PLAYER, "hello", false, 1_700_000_000_000L),
                new ChatRecord(null, 1, null, "lobby system line", true, 1_700_000_000_000L),
                new RoomStatusRecord("R-0001", RoomStatus.FINISHED, Colour.RED, 321)));

        assertEquals("alice", scalar("SELECT nickname FROM players WHERE id = 'p-1'"));
        assertEquals("2069", scalar("SELECT seed FROM rooms WHERE id = 'R-0001'"));
        assertEquals("FINISHED", scalar("SELECT status FROM rooms WHERE id = 'R-0001'"));
        assertEquals("RED", scalar("SELECT winner_colour FROM rooms WHERE id = 'R-0001'"));
        assertEquals("321", scalar("SELECT total_turns FROM rooms WHERE id = 'R-0001'"));
        assertNotNull(scalar("SELECT finished_at FROM rooms WHERE id = 'R-0001'"));
        assertEquals(2, count("seats"));
        assertNull(scalar("SELECT player_id FROM seats WHERE colour = 'GREEN'"));
        assertEquals(2, count("decisions"));
        assertNull(scalar("SELECT option_index FROM decisions WHERE turn_number = 0"), "a roll has no option");
        assertEquals("2", scalar("SELECT option_index FROM decisions WHERE turn_number = 1"));
        assertEquals(2, count("chat_messages"));
        assertEquals("hello", scalar("SELECT text FROM chat_messages WHERE room_id = 'R-0001'"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM chat_messages WHERE room_id IS NULL"));
    }

    @Test @DisplayName("a repeated decision, player or room is ignored, so retrying a batch is harmless")
    void writesAreIdempotent() throws Exception {
        List<WriteOp> batch = List.of(new PlayerRecord(PLAYER, "alice"), room("R-0001"), decision("R-0001", 5, 1));
        writer.writeAll(batch);
        writer.writeAll(batch);
        assertEquals(1, count("players"));
        assertEquals(1, count("rooms"));
        assertEquals(1, count("decisions"));
    }

    @Test @DisplayName("seats are replaced as a set, not appended")
    void seatsAreReplaced() throws Exception {
        writer.writeAll(List.of(new PlayerRecord(PLAYER, "alice"), room("R-0001")));
        for (Controller controller : List.of(Controller.HUMAN, Controller.AI)) {
            writer.writeAll(List.of(new SeatsRecord("R-0001",
                    List.of(new SeatsRecord.SeatRow(Colour.RED, controller == Controller.HUMAN ? PLAYER : null, controller)))));
        }
        assertEquals(1, count("seats"));
        assertEquals("AI", scalar("SELECT controller FROM seats"));
    }

    @Test @DisplayName("a batch is all-or-nothing: one bad row rolls the whole batch back")
    void batchIsAtomic() throws Exception {
        writer.writeAll(List.of(new PlayerRecord(PLAYER, "alice"), room("R-0001")));
        List<WriteOp> batch = new ArrayList<>();
        batch.add(decision("R-0001", 1, 0));
        batch.add(decision("R-9999", 2, 0)); // no such room: breaks the foreign key
        batch.add(decision("R-0001", 3, 0));
        assertThrows(PersistenceException.class, () -> writer.writeAll(batch));
        assertEquals(0, count("decisions"));
    }

    @Test @DisplayName("the writer recovers after a failed batch and keeps working")
    void recoversAfterFailure() throws Exception {
        assertThrows(PersistenceException.class, () -> writer.writeAll(List.of(decision("R-9999", 1, 0))));
        writer.writeAll(List.of(new PlayerRecord(PLAYER, "alice"), room("R-0001"), decision("R-0001", 1, 0)));
        assertEquals(1, count("decisions"));
    }

    @Test @DisplayName("order across kinds is kept: a player and room written first satisfy the rows that follow")
    void orderRespected() throws Exception {
        writer.writeAll(List.of(
                new PlayerRecord(PLAYER, "alice"),
                room("R-0001"),
                new ChatRecord("R-0001", 1, PLAYER, "first", false, 1L),
                decision("R-0001", 1, 0),
                new ChatRecord("R-0001", 2, PLAYER, "second", false, 2L),
                decision("R-0001", 2, 0)));
        assertEquals(2, count("chat_messages"));
        assertEquals(2, count("decisions"));
    }

    @Test @DisplayName("hundreds of decisions in one batch all arrive")
    void largeBatch() throws Exception {
        List<WriteOp> batch = new ArrayList<>(List.of(new PlayerRecord(PLAYER, "alice"), room("R-0001")));
        for (int turn = 0; turn < 500; turn++) {
            batch.add(decision("R-0001", turn, turn % 3 == 0 ? null : 0));
        }
        writer.writeAll(batch);
        assertEquals(500, count("decisions"));
    }

    @Test @DisplayName("a nickname or chat text longer than its column is refused, not truncated")
    void overlongValuesRefused() {
        assertThrows(PersistenceException.class,
                () -> writer.writeAll(List.of(new PlayerRecord(PLAYER, "this_nickname_is_far_too_long"))));
    }

    @Test @DisplayName("an unreachable database surfaces as a PersistenceException, not a crash")
    void unreachableDatabase() {
        JdbcBatchWriter broken = new JdbcBatchWriter(new Database(
                new DatabaseConfig("h2", "jdbc:h2:tcp://localhost:1/nothing", "sa", "")));
        assertThrows(PersistenceException.class, () -> broken.writeAll(List.of(new PlayerRecord(PLAYER, "alice"))));
    }
}
