package com.ludot.server;

import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.JdbcBatchWriter;
import com.ludot.server.adapters.jdbc.JdbcGameHistory;
import com.ludot.server.adapters.jdbc.RoomNumbers;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import com.ludot.server.adapters.persistence.AsyncGameRepository;
import com.ludot.server.app.GameHistory;
import com.ludot.server.app.GameRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Starts the server: {@code ServerMain [path/to/server.properties]}. With a database configured, every game
 * is stored through an asynchronous writer; without one, games live only in memory.
 */
public final class ServerMain {

    private static final Logger LOG = Logger.getLogger(ServerMain.class.getName());
    private static final String DEFAULT_CONFIG = "config/server.properties";

    private ServerMain() {
    }

    /** What the server stores into and reads from, and how to close it. */
    private record Storage(GameRepository repository, GameHistory history, long lastRoomNumber, Runnable close) {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerConfig config = ServerConfig.load(Path.of(args.length > 0 ? args[0] : DEFAULT_CONFIG));
        Storage storage = config.database().enabled() ? databaseStorage(config) : memoryStorage();
        GameServer server = new GameServer(config, storage.repository(), storage.lastRoomNumber(), storage.history());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
            storage.close().run();
        }, "shutdown"));
        server.start();
        server.awaitClose();
    }

    private static Storage memoryStorage() {
        LOG.warning("No database configured: games are not stored");
        return new Storage(new InMemoryGameRepository(), GameHistory.NONE, 0, () -> { });
    }

    private static Storage databaseStorage(ServerConfig config) {
        Database database = new Database(config.database());
        long lastRoomNumber = 0;
        try (Connection connection = database.open()) {
            lastRoomNumber = RoomNumbers.highest(connection);
        } catch (SQLException e) {
            System.err.println("Cannot use the database: " + e.getMessage()
                    + "\nCheck config/server.properties, or remove db.url to run without storage.");
            System.exit(1);
        }
        JdbcBatchWriter writer = new JdbcBatchWriter(database);
        AsyncGameRepository async = new AsyncGameRepository(writer);
        JdbcGameHistory history = new JdbcGameHistory(database);
        LOG.info("Storing games in the database (" + config.database().mode() + "); new rooms continue after R-"
                + String.format("%04d", lastRoomNumber));
        return new Storage(async, history, lastRoomNumber, () -> {
            history.close();
            async.close();
            writer.close();
        });
    }
}
