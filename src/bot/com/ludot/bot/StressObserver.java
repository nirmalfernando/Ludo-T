package com.ludot.bot;

import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.StateHash;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.zip.CRC32;

final class StressObserver implements Consumer<ServerMessage> {

    private final AtomicReference<ServerMessage.YourTurn> ourTurn = new AtomicReference<>();
    private final AtomicReference<ServerMessage.RoomSnapshot> lastSnapshot = new AtomicReference<>();
    private final AtomicLong updatesSeen = new AtomicLong();
    private final AtomicLong hashMismatches = new AtomicLong();
    private final AtomicLong chatOrderHash = new AtomicLong();
    private final AtomicLong chatLines = new AtomicLong();
    private final CRC32 chatCrc = new CRC32();     // touched only by the reader thread
    private volatile int latestTurn;
    private volatile String myId = "";
    private volatile String hostId = "";
    private volatile boolean finished;
    private volatile boolean sawUpdate;
    private volatile long lastUpdateHash;

    @Override
    public void accept(ServerMessage message) {
        if (message instanceof ServerMessage.Welcome welcome) {
            myId = welcome.playerId();
        } else if (message instanceof ServerMessage.YourTurn turn) {
            latestTurn = Math.max(latestTurn, turn.turnNumber());
            ourTurn.set(turn);
        } else if (message instanceof ServerMessage.StateUpdate update) {
            onUpdate(update);
        } else if (message instanceof ServerMessage.RoomSnapshot snapshot) {
            hostId = snapshot.hostId();
            lastSnapshot.set(snapshot);
        } else if (message instanceof ServerMessage.RoomStateChanged changed) {
            hostId = changed.hostId();
        } else if (message instanceof ServerMessage.ChatMessage chat) {
            onChat(chat);
        } else if (message instanceof ServerMessage.GameOver) {
            finished = true;
        }
    }

    private void onUpdate(ServerMessage.StateUpdate update) {
        int turn = update.turn().turnNumber();
        latestTurn = Math.max(latestTurn, turn);
        updatesSeen.incrementAndGet();
        if (StateHash.of(update.board(), turn) != update.stateHash()) {
            hashMismatches.incrementAndGet();
        }
        lastUpdateHash = update.stateHash();
        sawUpdate = true;
        ServerMessage.YourTurn pending = ourTurn.get();
        if (pending != null && pending.turnNumber() < turn) {
            ourTurn.compareAndSet(pending, null); // the game moved past it (the AI took over, or we were too slow)
        }
    }

    private void onChat(ServerMessage.ChatMessage chat) {
        if (chat.roomId() == null || chat.system()) {
            return;
        }
        chatCrc.update((chat.seq() + "|" + chat.senderId() + "|" + chat.text() + "\n").getBytes(StandardCharsets.UTF_8));
        chatOrderHash.set(chatCrc.getValue());
        chatLines.incrementAndGet();
    }

    // the offer for our seat's turn, handed to exactly one caller, or null if there is none to answer
    ServerMessage.YourTurn takeTurn() {
        return ourTurn.getAndSet(null);
    }

    int latestTurn() {
        return latestTurn;
    }

    boolean isHost() {
        return myId.equals(hostId);
    }

    boolean finished() {
        return finished;
    }

    boolean sawUpdate() {
        return sawUpdate;
    }

    long lastUpdateHash() {
        return lastUpdateHash;
    }

    ServerMessage.RoomSnapshot lastSnapshot() {
        return lastSnapshot.get();
    }

    long updatesSeen() {
        return updatesSeen.get();
    }

    long hashMismatches() {
        return hashMismatches.get();
    }

    long chatOrderHash() {
        return chatOrderHash.get();
    }

    long chatLines() {
        return chatLines.get();
    }
}
