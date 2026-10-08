package com.ludot.server.adapters.memory;

import com.ludot.server.app.ChatRecord;
import com.ludot.server.app.DecisionRecord;
import com.ludot.protocol.FinishedGame;
import com.ludot.protocol.FinishedGameList;
import com.ludot.protocol.RoomStatus;
import com.ludot.server.app.GameHistory;
import com.ludot.server.app.GameRepository;
import com.ludot.server.app.ReplayData;
import com.ludot.server.app.PlayerRecord;
import com.ludot.server.app.RoomRecord;
import com.ludot.server.app.RoomStatusRecord;
import com.ludot.server.app.WriteOp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryGameRepository implements GameRepository, GameHistory {

    private final List<WriteOp> operations = new CopyOnWriteArrayList<>();

    @Override
    public void record(WriteOp operation) {
        operations.add(operation);
    }

    public List<WriteOp> operations() {
        return List.copyOf(operations);
    }

    public List<DecisionRecord> decisions() {
        return of(DecisionRecord.class);
    }

    public List<ChatRecord> chats() {
        return of(ChatRecord.class);
    }

    public List<PlayerRecord> players() {
        return of(PlayerRecord.class);
    }

    public List<RoomRecord> rooms() {
        return of(RoomRecord.class);
    }

    public List<RoomStatusRecord> statuses() {
        return of(RoomStatusRecord.class);
    }

    // ------------------------------------------------------------------ GameHistory

    @Override
    public FinishedGameList finished(int page) {
        List<FinishedGame> all = new ArrayList<>();
        Map<String, RoomRecord> rooms = new HashMap<>();
        rooms().forEach(room -> rooms.put(room.roomId(), room));
        for (RoomStatusRecord status : statuses()) {
            RoomRecord room = rooms.get(status.roomId());
            if (status.status() == RoomStatus.FINISHED && status.winner() != null && room != null) {
                all.add(new FinishedGame(room.roomId(), room.mode(), room.variant(), room.seed(), status.winner(),
                        Instant.EPOCH, status.totalTurns()));
            }
        }
        java.util.Collections.reverse(all);
        int number = Math.max(0, page);
        int from = Math.min(all.size(), number * PAGE_SIZE);
        int to = Math.min(all.size(), from + PAGE_SIZE);
        return new FinishedGameList(all.subList(from, to), number, to < all.size());
    }

    @Override
    public Optional<ReplayData> load(String gameId) {
        Optional<RoomRecord> room = rooms().stream().filter(r -> r.roomId().equals(gameId)).findFirst();
        Optional<RoomStatusRecord> done = statuses().stream().filter(s -> s.roomId().equals(gameId)
                && s.status() == RoomStatus.FINISHED && s.winner() != null).findFirst();
        if (room.isEmpty() || done.isEmpty()) {
            return Optional.empty();
        }
        List<ReplayData.Step> steps = decisions().stream().filter(d -> d.roomId().equals(gameId))
                .sorted(java.util.Comparator.comparingInt(DecisionRecord::turnNumber))
                .map(d -> new ReplayData.Step(d.turnNumber(), d.colour(), d.roll(), d.optionIndex())).toList();
        return Optional.of(new ReplayData(gameId, room.get().variant(), room.get().seed(), done.get().winner(), steps));
    }

    private <T extends WriteOp> List<T> of(Class<T> type) {
        return operations.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
