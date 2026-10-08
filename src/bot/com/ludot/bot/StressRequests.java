package com.ludot.bot;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Phase;
import com.ludot.protocol.ServerMessage;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the requests the stress client sends: the real roll or move for our own turn, and the deliberately
 * wrong or awkward ones (stale, out of turn, bad index, not host, and so on) for each {@link RequestKind}.
 */
final class StressRequests {

    private static final int BAD_INDEX = 99;

    private final StressOptions options;
    private final StressObserver observer;
    private final BotStrategy strategy;
    private final AtomicInteger chatCounter = new AtomicInteger();
    private volatile String roomId;

    StressRequests(StressOptions options, StressObserver observer) {
        this.options = options;
        this.observer = observer;
        this.strategy = BotStrategies.forColour(options.seat(), new Random(options.randomSeed()));
    }

    /** Sets the room the requests are for, once it is known (it may have been created by this client). */
    void useRoom(String room) {
        this.roomId = room;
    }

    /** The real move for an offered turn: a roll, or the option the seat's style prefers. */
    ClientMessage.Request legitMove(ServerMessage.YourTurn turn) {
        return turn.phase() != Phase.AWAITING_MOVE // a roll, or the roll for the playing order
                ? new ClientMessage.Roll(UUID.randomUUID(), roomId, turn.turnNumber())
                : new ClientMessage.ChooseMove(UUID.randomUUID(), roomId, turn.turnNumber(),
                        strategy.choose(turn.options()));
    }

    ClientMessage.Request of(RequestKind kind) {
        UUID id = UUID.randomUUID();
        int latestTurn = observer.latestTurn();
        return switch (kind) {
            // our view lags the server's, so latestTurn - 5 is always behind the real turn (even if negative)
            case STALE_MOVE -> new ClientMessage.ChooseMove(id, roomId, latestTurn - 5, 0);
            case OUT_OF_TURN_ROLL -> new ClientMessage.Roll(id, roomId, latestTurn);
            case LIST_ROOMS -> new ClientMessage.ListRooms(id);
            case CHAT -> new ClientMessage.SendChat(id, roomId, options.name() + "#" + chatCounter.getAndIncrement());
            case CLAIM_TAKEN -> new ClientMessage.ClaimSeat(id, roomId, options.seat());
            case PAUSE_NOT_HOST -> new ClientMessage.PauseGame(id, roomId);
            case BAD_INDEX -> new ClientMessage.ChooseMove(id, roomId, latestTurn, BAD_INDEX);
            default -> new ClientMessage.RequestSnapshot(id, roomId);
        };
    }
}
