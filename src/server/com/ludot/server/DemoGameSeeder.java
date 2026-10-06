package com.ludot.server;

import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.Variant;
import com.ludot.server.app.ClientSink;
import com.ludot.server.app.GameRepository;
import com.ludot.server.app.GameSession;
import com.ludot.server.app.LobbyPublisher;
import com.ludot.server.app.Participant;
import com.ludot.server.app.PlayerIdentity;
import com.ludot.server.app.PlayerRecord;
import com.ludot.server.app.SessionConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Plays one complete Bot match at full speed through the real room and repository, so the database holds a
 * genuine finished game (valid moves, because the real engine made them) that replay can use straight away.
 */
public final class DemoGameSeeder {

    /** The id of the seeded demo person; matches db/seed.sql. */
    public static final String DEMO_PLAYER_ID = "00000000-0000-0000-0000-000000000001";

    private static final long TIME_LIMIT_SECONDS = 180;

    private DemoGameSeeder() {
    }

    /** Plays the game as {@code roomId} and returns once its GameOver has been produced. */
    public static void play(GameRepository repository, String roomId, long seed) throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        CountDownLatch finished = new CountDownLatch(1);
        ClientSink watcher = new ClientSink() {
            @Override
            public boolean offer(ServerMessage message) {
                if (message instanceof ServerMessage.GameOver) {
                    finished.countDown();
                }
                return true;
            }

            @Override
            public void close() {
                // nothing to release
            }
        };
        GameSession session = new GameSession(roomId, RoomMode.BOT_MATCH, Variant.LUDO_T, seed,
                SessionConfig.defaults().withDelays(0, 0, 0), scheduler, LobbyPublisher.NONE, repository,
                System::currentTimeMillis);
        try {
            PlayerIdentity host = new PlayerIdentity(DEMO_PLAYER_ID, "demo_alice", "demo-token");
            repository.record(new PlayerRecord(host.id(), host.nickname()));
            session.attachCreator(new Participant(host, watcher));
            if (!finished.await(TIME_LIMIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The demo game did not finish within " + TIME_LIMIT_SECONDS + " s");
            }
        } finally {
            session.shutdown();
            scheduler.shutdownNow();
        }
    }
}
