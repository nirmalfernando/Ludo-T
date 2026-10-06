package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import com.ludot.protocol.ServerMessage.LobbyUpdate;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Variant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The lobby and registry under simultaneous load. */
@DisplayName("Lobby concurrency")
class LobbyConcurrencyTest {

    private RouterKit kit;
    private ExecutorService pool = Executors.newCachedThreadPool();

    @AfterEach void tearDown() {
        pool.shutdownNow();
        if (kit != null) {
            kit.close();
        }
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    private <T> List<T> fireTogether(List<Callable<T>> tasks) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        go.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        return results;
    }

    private List<RouterKit.Client> subscribers(int count) {
        List<RouterKit.Client> clients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RouterKit.Client client = kit.hello("watcher" + i);
            client.call(new ClientMessage.ListRooms(id()));
            clients.add(client);
        }
        return clients;
    }

    private static LobbyUpdate lastUpdate(RouterKit.Client client) {
        return client.inbox.of(LobbyUpdate.class).stream().reduce((a, b) -> b).orElseThrow();
    }

    private static Set<String> ids(List<RoomSummary> rooms) {
        return rooms.stream().map(RoomSummary::roomId).collect(Collectors.toSet());
    }

    // CT-06
    @RepeatedTest(5) @DisplayName("100 rooms created at once get 100 distinct ids and the lobby ends up listing exactly them")
    void concurrentRoomCreation() throws Exception {
        kit = new RouterKit();
        RouterKit.Client watcher = subscribers(1).get(0);
        List<RouterKit.Client> creators = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            creators.add(kit.hello("creator" + i));
        }

        List<Callable<String>> creations = new ArrayList<>();
        for (RouterKit.Client creator : creators) {
            creations.add(() -> {
                Response response = creator.call(new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L));
                assertTrue(response.isOk());
                return (String) response.payload();
            });
        }
        List<String> roomIds = fireTogether(creations);

        assertEquals(100, new HashSet<>(roomIds).size(), "ids must be distinct");
        assertEquals(100, kit.registry.size());
        kit.lobby.awaitIdle();
        assertEquals(new HashSet<>(roomIds), ids(lastUpdate(watcher).rooms()));
    }

    // CT-09
    @RepeatedTest(5) @DisplayName("rooms created and closed at the same time: every subscriber ends with the registry's rooms")
    void lobbyBroadcastConsistency() throws Exception {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1).withEmptyRoomCloseMs(20));
        List<RouterKit.Client> watchers = subscribers(10);

        List<Callable<Boolean>> churn = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            RouterKit.Client creator = kit.hello("churn" + i);
            boolean leaves = i % 2 == 0;
            churn.add(() -> {
                String roomId = (String) creator.call(
                        new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L)).payload();
                if (leaves) {
                    creator.call(new ClientMessage.LeaveRoom(id(), roomId));
                }
                return leaves;
            });
        }
        fireTogether(churn);

        Kit.await(() -> kit.registry.size() == 20, 10_000);
        kit.lobby.awaitIdle();
        Set<String> expected = kit.registry.all().stream().map(GameSession::roomId).collect(Collectors.toSet());
        for (RouterKit.Client watcher : watchers) {
            assertEquals(expected, ids(lastUpdate(watcher).rooms()));
        }
    }

    @Test @DisplayName("when a room closes the lobby chat says so, after the 'created' line, once")
    void closingIsAnnounced() throws Exception {
        kit = new RouterKit(SessionConfig.defaults().withDelays(0, 1, 1).withEmptyRoomCloseMs(20));
        RouterKit.Client watcher = subscribers(1).get(0);
        RouterKit.Client creator = kit.hello("maker");
        String roomId = (String) creator.call(new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L)).payload();
        creator.call(new ClientMessage.LeaveRoom(id(), roomId));

        String closed = "Room " + roomId + " was closed";
        Kit.await(() -> watcher.inbox.of(ChatMessage.class).stream().anyMatch(m -> m.system() && m.text().equals(closed)), 5000);
        kit.lobby.awaitIdle();
        List<String> lines = watcher.inbox.of(ChatMessage.class).stream().filter(m -> m.roomId() == null).map(ChatMessage::text).toList();
        assertEquals(List.of("maker created Play room " + roomId, closed), lines);
    }

    // CT-12
    @RepeatedTest(5) @DisplayName("lobby chat during room creation: every subscriber sees the same gap-free order, system lines included")
    void lobbyChatOrdering() throws Exception {
        kit = new RouterKit();
        List<RouterKit.Client> watchers = subscribers(10);
        List<RouterKit.Client> chatters = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            RouterKit.Client chatter = kit.hello("chatter" + i);
            chatter.call(new ClientMessage.ListRooms(id()));
            chatters.add(chatter);
        }
        List<RouterKit.Client> creators = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            creators.add(kit.hello("maker" + i));
        }

        List<Callable<Boolean>> activity = new ArrayList<>();
        for (RouterKit.Client chatter : chatters) {
            activity.add(() -> {
                for (int n = 0; n < 5; n++) {
                    Response r = chatter.call(new ClientMessage.SendChat(id(), null, "line " + n));
                    assertTrue(r.isOk(), String.valueOf(r.message()));
                }
                return true;
            });
        }
        for (RouterKit.Client creator : creators) {
            activity.add(() -> creator.call(new ClientMessage.CreateRoom(id(), RoomMode.PLAY, Variant.LUDO_T, 1L)).isOk());
        }
        fireTogether(activity);
        // each room tells the lobby about itself from its own thread: let the rooms finish first, then the lobby
        for (GameSession room : kit.registry.all()) {
            room.awaitIdle();
        }
        kit.lobby.awaitIdle();

        List<List<String>> orders = new ArrayList<>();
        for (RouterKit.Client watcher : watchers) {
            orders.add(lobbyPushes(watcher));
        }
        assertFalse(orders.get(0).isEmpty());
        for (List<String> order : orders) {
            assertEquals(orders.get(0), order, "lobby order differs between subscribers");
        }
        assertEquals(50, countChat(watchers.get(0), false));
        assertEquals(20, countChat(watchers.get(0), true));
        assertGapFree(watchers.get(0));
    }

    // "<seq>:<kind>" for every lobby push, in arrival order
    private static List<String> lobbyPushes(RouterKit.Client client) {
        List<String> pushes = new ArrayList<>();
        for (ServerMessage message : client.inbox.messages()) {
            if (message instanceof LobbyUpdate update) {
                pushes.add(update.lobbySeq() + ":rooms");
            } else if (message instanceof ChatMessage chat && chat.roomId() == null) {
                pushes.add(chat.seq() + ":chat:" + chat.text());
            }
        }
        return pushes;
    }

    private static long countChat(RouterKit.Client client, boolean system) {
        return client.inbox.of(ChatMessage.class).stream()
                .filter(m -> m.roomId() == null && m.system() == system && (system ? m.text().contains("created") : true))
                .count();
    }

    private static void assertGapFree(RouterKit.Client client) {
        long previous = -1;
        for (String push : lobbyPushes(client)) {
            long seq = Long.parseLong(push.substring(0, push.indexOf(':')));
            if (previous >= 0) {
                assertEquals(previous + 1, seq, "gap in the lobby sequence");
            }
            previous = seq;
        }
    }
}
