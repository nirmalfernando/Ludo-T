package com.ludot.server.adapters;

import com.ludot.protocol.Colour;
import com.ludot.protocol.FinishedGame;
import com.ludot.protocol.FinishedGameList;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.Variant;
import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.DatabaseConfig;
import com.ludot.server.adapters.jdbc.JdbcBatchWriter;
import com.ludot.server.adapters.jdbc.JdbcGameHistory;
import com.ludot.server.adapters.jdbc.SqlScripts;
import com.ludot.server.app.DecisionRecord;
import com.ludot.server.app.DecisionSource;
import com.ludot.server.app.GameHistory;
import com.ludot.server.app.PlayerRecord;
import com.ludot.server.app.ReplayData;
import com.ludot.server.app.RoomRecord;
import com.ludot.server.app.RoomStatusRecord;
import com.ludot.server.app.WriteOp;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JdbcGameHistory (finished games from the database)")
class JdbcGameHistoryTest {

    private static final String PLAYER = "p-1";

    private Database database;
    private Connection keepAlive;
    private JdbcBatchWriter writer;
    private JdbcGameHistory history;

    @BeforeEach void setUp() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        database = new Database(new DatabaseConfig("h2", url, "sa", ""));
        keepAlive = database.open();
        SqlScripts.run(keepAlive, Path.of("db", "schema.sql"));
        writer = new JdbcBatchWriter(database);
        writer.writeAll(List.of(new PlayerRecord(PLAYER, "alice")));
        history = new JdbcGameHistory(database);
    }

    @AfterEach void tearDown() throws Exception {
        history.close();
        writer.close();
        keepAlive.close();
    }

    private void room(String id, Variant variant, long seed) throws Exception {
        writer.writeAll(List.of(new RoomRecord(id, RoomMode.BOT_MATCH, variant, seed, PLAYER)));
    }

    private void finish(String id, Colour winner, int turns) throws Exception {
        writer.writeAll(List.of(new RoomStatusRecord(id, RoomStatus.FINISHED, winner, turns)));
    }

    private void finishedGame(String id, Colour winner) throws Exception {
        room(id, Variant.LUDO_T, 7);
        finish(id, winner, 100);
    }

    @Test @DisplayName("only finished games are listed: waiting, running and abandoned rooms are not")
    void listsOnlyFinishedGames() throws Exception {
        finishedGame("R-0001", Colour.RED);
        room("R-0002", Variant.LUDO_T, 1);                                   // created, never started
        room("R-0003", Variant.CLASSIC, 2);
        writer.writeAll(List.of(new RoomStatusRecord("R-0003", RoomStatus.RUNNING, null, 12)));
        room("R-0004", Variant.CLASSIC, 3);
        writer.writeAll(List.of(new RoomStatusRecord("R-0004", RoomStatus.CLOSED, null, 4)));

        FinishedGameList list = history.finished(0);
        assertEquals(1, list.games().size());
        FinishedGame game = list.games().get(0);
        assertEquals("R-0001", game.gameId());
        assertEquals(Colour.RED, game.winner());
        assertEquals(Variant.LUDO_T, game.variant());
        assertEquals(RoomMode.BOT_MATCH, game.mode());
        assertEquals(7, game.seed());
        assertEquals(100, game.turns());
        assertFalse(list.hasMore());
    }

    @Test @DisplayName("pages hold eight games, newest first, and say whether another page follows")
    void paging() throws Exception {
        for (int i = 1; i <= 11; i++) {
            finishedGame(String.format("R-%04d", i), Colour.values()[i % 4]);
        }
        FinishedGameList first = history.finished(0);
        assertEquals(GameHistory.PAGE_SIZE, first.games().size());
        assertTrue(first.hasMore());
        assertEquals("R-0011", first.games().get(0).gameId());
        assertEquals("R-0004", first.games().get(7).gameId());

        FinishedGameList second = history.finished(1);
        assertEquals(3, second.games().size());
        assertFalse(second.hasMore());
        assertEquals("R-0003", second.games().get(0).gameId());
        assertEquals(1, second.page());

        assertTrue(history.finished(5).games().isEmpty());
        assertEquals(0, history.finished(-3).page(), "a negative page counts as the first");
    }

    @Test @DisplayName("exactly one full page does not claim there is another")
    void exactFullPage() throws Exception {
        for (int i = 1; i <= GameHistory.PAGE_SIZE; i++) {
            finishedGame(String.format("R-%04d", i), Colour.BLUE);
        }
        FinishedGameList list = history.finished(0);
        assertEquals(GameHistory.PAGE_SIZE, list.games().size());
        assertFalse(list.hasMore());
    }

    @Test @DisplayName("a game loads with its rules, seed, winner and decisions in step order, keeping empty rolls and options")
    void loadsTheDecisions() throws Exception {
        room("R-0001", Variant.CLASSIC, 2069);
        List<WriteOp> decisions = new ArrayList<>();
        decisions.add(new DecisionRecord("R-0001", 3, Colour.GREEN, 4, 1, DecisionSource.HUMAN));
        decisions.add(new DecisionRecord("R-0001", 1, Colour.RED, 6, null, DecisionSource.AI));
        decisions.add(new DecisionRecord("R-0001", 2, Colour.RED, null, 0, DecisionSource.TIMEOUT));
        writer.writeAll(decisions);
        finish("R-0001", Colour.GREEN, 3);

        ReplayData data = history.load("R-0001").orElseThrow();
        assertEquals(Variant.CLASSIC, data.variant());
        assertEquals(2069, data.seed());
        assertEquals(Colour.GREEN, data.winner());
        assertEquals(List.of(1, 2, 3), data.steps().stream().map(ReplayData.Step::turnNumber).toList());
        assertEquals(6, data.steps().get(0).roll());
        assertNull(data.steps().get(0).optionIndex(), "a roll step has no option");
        assertNull(data.steps().get(1).roll());
        assertEquals(0, data.steps().get(1).optionIndex());
        assertEquals(Colour.GREEN, data.steps().get(2).colour());
    }

    @Test @DisplayName("a game that is unknown, or not finished, cannot be loaded")
    void unknownOrUnfinished() throws Exception {
        room("R-0001", Variant.LUDO_T, 1);
        assertEquals(Optional.empty(), history.load("R-0001"));
        assertEquals(Optional.empty(), history.load("R-9999"));
    }

    @Test @DisplayName("an empty database gives an empty first page")
    void nothingYet() {
        FinishedGameList list = history.finished(0);
        assertTrue(list.games().isEmpty());
        assertFalse(list.hasMore());
    }
}
