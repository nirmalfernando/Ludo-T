package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ReplayEndReason;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.ServerMessage.ReplayEnded;
import com.ludot.protocol.ServerMessage.ReplayEvent;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.DatabaseConfig;
import com.ludot.server.adapters.jdbc.JdbcBatchWriter;
import com.ludot.server.adapters.jdbc.JdbcGameHistory;
import com.ludot.server.adapters.jdbc.SqlScripts;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The whole chain: a played game is stored as SQL rows, read back by JdbcGameHistory and replayed to the same end. */
@DisplayName("A game stored in the database replays to the same result")
class ReplayFromDatabaseTest {

    @Test @DisplayName("stored in H2, loaded through JDBC and replayed: same winner and same final board")
    void databaseToReplay() throws Exception {
        ReplayFixtures.Played played = ReplayFixtures.play("R-0001", Variant.LUDO_T, 2069L);
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Database database = new Database(new DatabaseConfig("h2", url, "sa", ""));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        try (Connection keepAlive = database.open(); JdbcBatchWriter writer = new JdbcBatchWriter(database);
             JdbcGameHistory history = new JdbcGameHistory(database)) {
            SqlScripts.run(keepAlive, Path.of("db", "schema.sql"));
            writer.writeAll(List.of(new PlayerRecord("host", "host_h"),
                    new RoomRecord("R-0001", RoomMode.BOT_MATCH, Variant.LUDO_T, 2069L, "host")));
            List<WriteOp> decisions = new ArrayList<>();
            for (ReplayData.Step step : played.data().steps()) {
                decisions.add(new DecisionRecord("R-0001", step.turnNumber(), step.colour(), step.roll(),
                        step.optionIndex(), DecisionSource.AI));
            }
            writer.writeAll(decisions);
            writer.writeAll(List.of(new RoomStatusRecord("R-0001", RoomStatus.FINISHED, played.winner(),
                    played.data().steps().size())));

            RecordingSink sink = new RecordingSink();
            Participant viewer = new Participant(new PlayerIdentity("v", "viewer", "t"), sink);
            ReplayService service = new ReplayService(history, scheduler, speed -> 0L);
            List<Object> answers = new ArrayList<>();
            service.start(viewer, new ClientMessage.StartReplay(UUID.randomUUID(), "R-0001", Speed.FAST), answers::add);

            ReplayEnded ended = sink.await(ReplayEnded.class, 30_000);
            assertEquals(ReplayEndReason.COMPLETED, ended.reason());
            List<ReplayEvent> events = sink.of(ReplayEvent.class);
            assertEquals(played.finalBoard(), events.get(events.size() - 1).board());
            assertEquals(played.data().steps().size() + 1, events.size());
            assertTrue(history.finished(0).games().stream().anyMatch(g -> g.winner() == played.winner()));
        } finally {
            scheduler.shutdownNow();
        }
    }
}
