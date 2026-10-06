package com.ludot.server;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServerConfig")
class ServerConfigTest {

    @Test @DisplayName("a missing file gives the documented defaults: 30 s turns, away after 2 misses, no database")
    void defaultsWhenMissing() throws Exception {
        ServerConfig config = ServerConfig.load(Path.of("does-not-exist.properties"));
        assertEquals(5555, config.port());
        assertEquals(30_000, config.session().turnTimeoutMs());
        assertEquals(2, config.session().afkAfterTimeouts());
        assertEquals(1_024, config.session().requestMemory());
        assertEquals(256, config.maxInFlight());
        assertFalse(config.database().enabled());
    }

    @Test @DisplayName("timer, tuning and database settings are read from the file")
    void readsSettings() throws Exception {
        Path file = Files.createTempFile("ludot", ".properties");
        try {
            Files.writeString(file, "server.port=6000\nturn.timeoutSeconds=12\nafk.timeoutsBeforeAfk=3\n"
                    + "room.requestMemory=2048\nspeed.fastMs=10\nconnection.maxInFlight=64\n"
                    + "db.url=jdbc:postgresql://host:5432/postgres\ndb.user=u\ndb.password=p\n");
            ServerConfig config = ServerConfig.load(file);
            assertEquals(6000, config.port());
            assertEquals(12_000, config.session().turnTimeoutMs());
            assertEquals(3, config.session().afkAfterTimeouts());
            assertEquals(2_048, config.session().requestMemory());
            assertEquals(10, config.session().fastMs());
            assertEquals(64, config.maxInFlight());
            assertTrue(config.database().enabled());
            assertEquals("postgres", config.database().mode());
            assertEquals("u", config.database().user());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test @DisplayName("db.mode none switches the database off even when a url is present")
    void modeNoneDisables() throws Exception {
        Path file = Files.createTempFile("ludot", ".properties");
        try {
            Files.writeString(file, "db.mode=none\ndb.url=jdbc:postgresql://host:5432/postgres\n");
            assertFalse(ServerConfig.load(file).database().enabled());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
