package com.ludot.server;

import com.ludot.server.adapters.jdbc.Database;
import com.ludot.server.adapters.jdbc.JdbcBatchWriter;
import com.ludot.server.adapters.jdbc.RoomNumbers;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import com.ludot.server.adapters.persistence.AsyncGameRepository;
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

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerConfig config = ServerConfig.load(Path.of(args.length > 0 ? args[0] : DEFAULT_CONFIG));
        AsyncGameRepository async = null;
        JdbcBatchWriter writer = null;
        GameRepository repository;
        long lastRoomNumber = 0;
        if (config.database().enabled()) {
            Database database = new Database(config.database());
            try (Connection connection = database.open()) {
                lastRoomNumber = RoomNumbers.highest(connection);
            } catch (SQLException e) {
                System.err.println("Cannot use the database: " + e.getMessage()
                        + "\nCheck config/server.properties, or remove db.url to run without storage.");
                System.exit(1);
            }
            writer = new JdbcBatchWriter(database);
            async = new AsyncGameRepository(writer);
            repository = async;
            LOG.info("Storing games in the database (" + config.database().mode() + "); new rooms continue after R-"
                    + String.format("%04d", lastRoomNumber));
        } else {
            repository = new InMemoryGameRepository();
            LOG.warning("No database configured: games are not stored");
        }
        GameServer server = new GameServer(config, repository, lastRoomNumber);
        AsyncGameRepository finalAsync = async;
        JdbcBatchWriter finalWriter = writer;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
            if (finalAsync != null) {
                finalAsync.close();
                finalWriter.close();
            }
        }, "shutdown"));
        server.start();
        server.awaitClose();
    }
}
