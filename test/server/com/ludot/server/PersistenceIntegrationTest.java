package com.ludot.server;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerLink;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.DatabaseConfig;
import com.ludot.server.adapters.jdbc.JdbcBatchWriter;
import com.ludot.server.adapters.jdbc.RoomNumbers;
import com.ludot.server.adapters.jdbc.SqlScripts;
import com.ludot.server.adapters.persistence.AsyncGameRepository;
import com.ludot.server.app.BatchWriter;
import com.ludot.server.app.PersistenceException;
import com.ludot.server.app.SessionConfig;
import com.ludot.server.app.WriteOp;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real games over a real server, stored through the real asynchronous writer into an H2 database. */
@DisplayName("Persistence end to end")
class PersistenceIntegrationTest {

    private Database database;
    private Connection reader;
    private JdbcBatchWriter jdbc;
    private AsyncGameRepository repository;
    private GameServer server;
    private final List<ServerLink> links = new ArrayList<>();

    @BeforeEach void setUp() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        database = new Database(new DatabaseConfig("h2", url, "sa", ""));
        reader = database.open();
        SqlScripts.run(reader, Path.of("db", "schema.sql"));
        jdbc = new JdbcBatchWriter(database);
    }

    @AfterEach void tearDown() throws Exception {
        links.forEach(ServerLink::close);
        if (server != null) {
            server.close();
        }
        if (repository != null) {
            repository.close();
        }
        jdbc.close();
        reader.close();
    }

    private void startServer(BatchWriter writer, long lastRoomNumber) throws Exception {
        repository = new AsyncGameRepository(writer);
        server = new GameServer(ServerConfig.defaults().withPort(0)
                .withSession(SessionConfig.defaults().withDelays(2, 2, 2)), repository, lastRoomNumber);
        server.start();
    }

    private ServerLink connect(String nickname) throws Exception {
        ServerLink link = ServerLink.connect("localhost", server.port());
        links.add(link);
        assertTrue(link.request(new ClientMessage.Hello(UUID.randomUUID(), nickname)).get(5, TimeUnit.SECONDS).isOk());
        return link;
    }

    /** Creates a Bot match and returns its id plus a future that completes when the game is over. */
    private String botMatch(ServerLink host, long seed, CompletableFuture<Void> over) throws Exception {
        host.addListener(message -> {
            if (message instanceof ServerMessage.GameOver) {
                over.complete(null);
            }
        });
        return (String) host.request(new ClientMessage.CreateRoom(UUID.randomUUID(), RoomMode.BOT_MATCH, Variant.LUDO_T, seed))
                .get(5, TimeUnit.SECONDS).payload();
    }

    private long scalar(String sql) throws SQLException {
        try (Statement s = reader.createStatement(); ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getLong(1);
        }
    }

    @Test @DisplayName("a finished bot match is stored completely: room, players, seats, every step in order, chat")
    void finishedGameIsStored() throws Exception {
        startServer(jdbc, 0);
        ServerLink host = connect("alice");
        CompletableFuture<Void> over = new CompletableFuture<>();
        String roomId = botMatch(host, 1L, over);
        over.get(60, TimeUnit.SECONDS);
        assertTrue(repository.awaitDrained(30_000));

        assertEquals(0, repository.stats().dropped());
        assertEquals(1, scalar("SELECT COUNT(*) FROM players WHERE nickname = 'alice'"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM rooms WHERE id = '" + roomId + "' AND status = 'FINISHED' AND seed = 1"));
        long totalTurns = scalar("SELECT total_turns FROM rooms WHERE id = '" + roomId + "'");
        assertTrue(totalTurns > 100);
        assertNotEquals(0, scalar("SELECT COUNT(*) FROM rooms WHERE id = '" + roomId + "' AND winner_colour IS NOT NULL"));
        assertEquals(4, scalar("SELECT COUNT(*) FROM seats WHERE room_id = '" + roomId + "' AND controller = 'AI'"));

        // one stored step for every roll and move, numbered 0 .. N-1 with no gaps
        assertEquals(totalTurns, scalar("SELECT COUNT(*) FROM decisions WHERE room_id = '" + roomId + "'"));
        assertEquals(0, scalar("SELECT MIN(turn_number) FROM decisions WHERE room_id = '" + roomId + "'"));
        assertEquals(totalTurns - 1, scalar("SELECT MAX(turn_number) FROM decisions WHERE room_id = '" + roomId + "'"));
        assertTrue(scalar("SELECT COUNT(*) FROM chat_messages WHERE room_id = '" + roomId + "' AND is_system") >= 2);
        assertTrue(scalar("SELECT COUNT(*) FROM chat_messages WHERE room_id IS NULL") >= 1, "the lobby announcement is stored too");
    }

    @Test @DisplayName("two games at once are stored without mixing or losing steps")
    void concurrentGamesStayApart() throws Exception {
        startServer(jdbc, 0);
        ServerLink a = connect("alice");
        ServerLink b = connect("bobby");
        CompletableFuture<Void> overA = new CompletableFuture<>();
        CompletableFuture<Void> overB = new CompletableFuture<>();
        String roomA = botMatch(a, 1L, overA);
        String roomB = botMatch(b, 2L, overB);
        CompletableFuture.allOf(overA, overB).get(90, TimeUnit.SECONDS);
        assertTrue(repository.awaitDrained(30_000));

        for (String room : List.of(roomA, roomB)) {
            long turns = scalar("SELECT total_turns FROM rooms WHERE id = '" + room + "'");
            assertEquals(turns, scalar("SELECT COUNT(*) FROM decisions WHERE room_id = '" + room + "'"), room);
            assertEquals(turns - 1, scalar("SELECT MAX(turn_number) FROM decisions WHERE room_id = '" + room + "'"), room);
        }
        assertEquals(0, repository.stats().dropped());
    }

    @Test @DisplayName("a restarted server carries on numbering after the stored rooms, so ids are never reused")
    void roomNumbersContinue() throws Exception {
        startServer(jdbc, 0);
        ServerLink host = connect("alice");
        CompletableFuture<Void> over = new CompletableFuture<>();
        String first = botMatch(host, 1L, over);
        over.get(60, TimeUnit.SECONDS);
        assertTrue(repository.awaitDrained(30_000));
        long highest = RoomNumbers.highest(reader);
        assertEquals("R-" + String.format("%04d", highest), first);

        links.forEach(ServerLink::close);
        links.clear();
        server.close();
        repository.close();
        startServer(jdbc, highest);
        ServerLink again = connect("alice");
        String second = botMatch(again, 3L, new CompletableFuture<>());
        assertEquals("R-" + String.format("%04d", highest + 1), second);
        assertNotEquals(first, second);
    }

    @Test @DisplayName("a slow database never slows the game: the game ends while the writes are still draining")
    void slowDatabaseDoesNotSlowTheGame() throws Exception {
        BatchWriter slow = new BatchWriter() {
            @Override
            public void writeAll(List<WriteOp> operations) throws PersistenceException {
                try {
                    Thread.sleep(150); // a distant database: every write takes 150 ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                jdbc.writeAll(operations);
            }
        };
        startServer(slow, 0);
        ServerLink host = connect("alice");
        CompletableFuture<Void> over = new CompletableFuture<>();
        long start = System.nanoTime();
        String roomId = botMatch(host, 1L, over);
        over.get(60, TimeUnit.SECONDS);
        long gameMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        AsyncGameRepository.Stats atEnd = repository.stats();
        assertTrue(atEnd.queueDepth() > 0, "the game finished before its log was fully written");
        // storing the whole game needs many 150 ms writes; a synchronous design would take at least that long per step
        long totalSteps = 1_000;
        assertTrue(gameMs < totalSteps * 150 / 4, "the game took " + gameMs + " ms: it must not wait for the database");

        assertTrue(repository.awaitDrained(60_000));
        long turns = scalar("SELECT total_turns FROM rooms WHERE id = '" + roomId + "'");
        assertEquals(turns, scalar("SELECT COUNT(*) FROM decisions WHERE room_id = '" + roomId + "'"));
    }
}
