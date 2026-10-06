package com.ludot.server.adapters.memory;

import com.ludot.server.app.ChatRecord;
import com.ludot.server.app.DecisionRecord;
import com.ludot.server.app.GameRepository;
import com.ludot.server.app.PlayerRecord;
import com.ludot.server.app.RoomRecord;
import com.ludot.server.app.RoomStatusRecord;
import com.ludot.server.app.WriteOp;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps everything in memory; used by tests and as the stand-in until the database is wired in. */
public final class InMemoryGameRepository implements GameRepository {

    private final List<WriteOp> operations = new CopyOnWriteArrayList<>();

    @Override
    public void record(WriteOp operation) {
        operations.add(operation);
    }

    /** Everything recorded, in order. */
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

    private <T extends WriteOp> List<T> of(Class<T> type) {
        return operations.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
