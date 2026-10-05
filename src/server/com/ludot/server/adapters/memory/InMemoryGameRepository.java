package com.ludot.server.adapters.memory;

import com.ludot.server.app.ChatRecord;
import com.ludot.server.app.DecisionRecord;
import com.ludot.server.app.GameRepository;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps everything in memory; used by tests and as the stand-in until the database is wired in. */
public final class InMemoryGameRepository implements GameRepository {

    private final List<DecisionRecord> decisions = new CopyOnWriteArrayList<>();
    private final List<ChatRecord> chats = new CopyOnWriteArrayList<>();

    @Override
    public void recordDecision(DecisionRecord decision) {
        decisions.add(decision);
    }

    @Override
    public void recordChat(ChatRecord chat) {
        chats.add(chat);
    }

    public List<DecisionRecord> decisions() {
        return List.copyOf(decisions);
    }

    public List<ChatRecord> chats() {
        return List.copyOf(chats);
    }
}
