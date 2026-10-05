package com.ludot.server;

import com.ludot.server.adapters.memory.InMemoryGameRepository;
import java.io.IOException;
import java.nio.file.Path;

/** Starts the server: {@code ServerMain [path/to/server.properties]}. */
public final class ServerMain {

    private static final String DEFAULT_CONFIG = "config/server.properties";

    private ServerMain() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerConfig config = ServerConfig.load(Path.of(args.length > 0 ? args[0] : DEFAULT_CONFIG));
        GameServer server = new GameServer(config, new InMemoryGameRepository());
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "shutdown"));
        server.start();
        server.awaitClose();
    }
}
