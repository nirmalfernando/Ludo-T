package com.ludot.server;

import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.DatabaseConfig;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DbTool")
class DbToolTest {

    private static Database memory() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        return new Database(new DatabaseConfig("h2", url, "sa", ""));
    }

    @Test @DisplayName("reset leaves the initial state: demo people and one complete finished game, which verify accepts")
    void resetThenVerify() throws Exception {
        Database database = memory();
        assertTrue(DbTool.run("reset", database));
        assertTrue(DbTool.verify(database, "R-0001"));
        assertTrue(DbTool.verify(database, null), "with no id the latest finished room is checked");
    }

    @Test @DisplayName("verify fails when a stored step is missing")
    void verifyDetectsAGap() throws Exception {
        Database database = memory();
        DbTool.run("reset", database);
        try (Connection connection = database.open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM decisions WHERE room_id = 'R-0001' AND turn_number = 100");
        }
        assertFalse(DbTool.verify(database, "R-0001"));
    }

    @Test @DisplayName("verify fails for a room that is not stored, and when nothing is finished")
    void verifyDetectsMissingRooms() throws Exception {
        Database database = memory();
        DbTool.run("schema", database);
        assertFalse(DbTool.verify(database, "R-9999"));
        assertFalse(DbTool.verify(database, null));
    }

    @Test @DisplayName("a second demo game gets the next room number instead of colliding with the first")
    void demoGamesDoNotCollide() throws Exception {
        Database database = memory();
        DbTool.run("reset", database);
        DbTool.run("demo", database);
        assertTrue(DbTool.verify(database, "R-0001"));
        assertTrue(DbTool.verify(database, "R-0002"));
    }
}
