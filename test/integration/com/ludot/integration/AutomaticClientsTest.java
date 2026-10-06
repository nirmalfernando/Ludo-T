package com.ludot.integration;

import com.ludot.bot.BotClient;
import com.ludot.bot.BotStrategies;
import com.ludot.bot.StressClient;
import com.ludot.bot.StressOptions;
import com.ludot.bot.StressReport;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.ServerLink;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import com.ludot.server.GameServer;
import com.ludot.server.ServerConfig;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import com.ludot.server.app.DecisionRecord;
import com.ludot.server.app.SessionConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The automatic clients against a real server on a random port, all over TCP. */
@DisplayName("Bot and stress clients against a live server")
class AutomaticClientsTest {

    private GameServer server;
    private InMemoryGameRepository repository;
    private ExecutorService pool;

    @BeforeEach void startServer() throws Exception {
        repository = new InMemoryGameRepository();
        server = new GameServer(ServerConfig.defaults().withPort(0)
                .withSession(SessionConfig.defaults().withDelays(0, 1, 1)), repository);
        server.start();
        pool = Executors.newCachedThreadPool();
    }

    @AfterEach void stopServer() {
        pool.shutdownNow();
        server.close();
    }

    private ServerLink connect() throws Exception {
        return ServerLink.connect("localhost", server.port());
    }

    @Test @DisplayName("four bots, one per colour, play a Play room over TCP to the end")
    void fourBotsFinishAGame() throws Exception {
        try (ServerLink operator = connect()) {
            operator.request(new ClientMessage.Hello(UUID.randomUUID(), "operator")).get(5, TimeUnit.SECONDS);
            Response created = operator.request(new ClientMessage.CreateRoom(UUID.randomUUID(), RoomMode.PLAY,
                    Variant.LUDO_T, 1L)).get(5, TimeUnit.SECONDS);
            String roomId = (String) created.payload();

            List<Future<BotClient.Result>> bots = new ArrayList<>();
            List<ServerLink> links = new ArrayList<>();
            for (Colour colour : Colour.values()) {
                ServerLink link = connect();
                links.add(link);
                BotClient bot = new BotClient(link, roomId, colour, 0,
                        BotStrategies.forColour(colour, new Random(colour.ordinal())), line -> { });
                Callable<BotClient.Result> play = () -> bot.play("bot_" + colour.name().toLowerCase(), 120);
                bots.add(pool.submit(play));
            }
            com.ludot.server.app.GameSession room = server.registry().find(roomId);
            org.junit.jupiter.api.Assertions.assertNotNull(room);
            waitForSeats(room);
            assertTrue(operator.request(new ClientMessage.StartGame(UUID.randomUUID(), roomId))
                    .get(5, TimeUnit.SECONDS).isOk());

            for (Future<BotClient.Result> bot : bots) {
                BotClient.Result result = bot.get(150, TimeUnit.SECONDS);
                assertEquals(4, result.standings().size());
                assertTrue(result.moves() > 0);
            }
            links.forEach(ServerLink::close);
        }
        assertFalse(repository.decisions().isEmpty());
        List<DecisionRecord> decisions = repository.decisions();
        assertTrue(decisions.stream().allMatch(d -> d.source() != null));
    }

    // the bots join and claim concurrently; starting before all four seats are taken would hand them to the AI
    private void waitForSeats(com.ludot.server.app.GameSession room) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (room.summary().humanSeats() < 4 && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertEquals(4, room.summary().humanSeats());
    }

    @Test @DisplayName("a bot told to take a seat that is already taken is refused with SEAT_TAKEN")
    void botRefusedSeat() throws Exception {
        try (ServerLink operator = connect(); ServerLink loser = connect()) {
            operator.request(new ClientMessage.Hello(UUID.randomUUID(), "operator")).get(5, TimeUnit.SECONDS);
            String roomId = (String) operator.request(new ClientMessage.CreateRoom(UUID.randomUUID(), RoomMode.PLAY,
                    Variant.LUDO_T, 1L)).get(5, TimeUnit.SECONDS).payload();
            operator.request(new ClientMessage.ClaimSeat(UUID.randomUUID(), roomId, Colour.RED)).get(5, TimeUnit.SECONDS);

            BotClient bot = new BotClient(loser, roomId, Colour.RED, 0,
                    BotStrategies.forColour(Colour.RED, new Random(1)), line -> { });
            BotClient.RefusedException refused = assertThrows(BotClient.RefusedException.class,
                    () -> bot.play("latecomer", 5));
            assertEquals(ErrorCode.SEAT_TAKEN, refused.code());
        }
    }

    @Test @DisplayName("two stress clients flood one room at the same time: no unexpected answers, no timeouts, identical final state and chat order")
    void twoStressClients() throws Exception {
        long startAt = System.currentTimeMillis() + 1_500;
        StressOptions a = options(null, true, "stressA", Colour.RED, startAt, true);
        StressClient clientA = new StressClient(a, line -> { });
        Future<StressReport> reportA = pool.submit(clientA::run);

        long deadline = System.currentTimeMillis() + 10_000;
        while (clientA.roomId() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertNotNull(clientA.roomId(), "stressA should have created the room");

        StressClient clientB = new StressClient(options(clientA.roomId(), false, "stressB", Colour.BLUE, startAt, false),
                line -> { });
        Future<StressReport> reportB = pool.submit(clientB::run);

        StressReport first = reportA.get(180, TimeUnit.SECONDS);
        StressReport second = reportB.get(180, TimeUnit.SECONDS);
        System.out.println(first);
        System.out.println(second);

        for (StressReport report : List.of(first, second)) {
            assertEquals(0, report.unexpected(), report.toString());
            assertEquals(0, report.timeouts(), report.toString());
            assertEquals(report.sent(), report.responses(), "every request must be answered");
            assertEquals(0, report.hashMismatches(), "every update must match its own state hash");
            assertTrue(report.finalHash().contains("MATCH") && !report.finalHash().contains("MISMATCH"), report.finalHash());
            assertTrue(report.updatesSeen() > 0);
        }
        assertEquals(first.chatOrderHash(), second.chatOrderHash(), "both clients must see the same chat order");
        assertEquals(first.chatLinesSeen(), second.chatLinesSeen());
        assertTrue(first.chatLinesSeen() > 0, "the flood should include chat");
    }

    private StressOptions options(String roomId, boolean create, String name, Colour seat, long startAt, boolean startGame) {
        return new StressOptions("localhost", server.port(), roomId, create, name, 600, 4, seat, 0, startAt,
                startGame, create ? Speed.FAST : null, 120, 99);
    }
}
