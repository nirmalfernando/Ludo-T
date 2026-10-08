package com.ludot.bot;

import com.ludot.protocol.ServerLink;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.Phase;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class BotClient {

    public record Result(int rolls, int moves, List<Colour> standings) {
    }

    public static final class RefusedException extends Exception {
        private static final long serialVersionUID = 1L;
        private final transient ErrorCode code;

        RefusedException(ErrorCode code, String message) {
            super(code + ": " + message);
            this.code = code;
        }

        public ErrorCode code() {
            return code;
        }
    }

    private static final long SETUP_TIMEOUT_SECONDS = 10;

    private final ServerLink link;
    private final String roomId;
    private final Colour colour;
    private final long delayMs;
    private final BotStrategy strategy;
    private final Consumer<String> log;
    private final ScheduledExecutorService actions = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "bot-actions");
        thread.setDaemon(true);
        return thread;
    });
    private final CompletableFuture<List<Colour>> gameOver = new CompletableFuture<>();
    private int rolls;
    private int moves;

    public BotClient(ServerLink link, String roomId, Colour colour, long delayMs, BotStrategy strategy,
                     Consumer<String> log) {
        this.link = link;
        this.roomId = roomId;
        this.colour = colour;
        this.delayMs = delayMs;
        this.strategy = strategy;
        this.log = log;
    }

    public Result play(String nickname, long timeoutSeconds) throws Exception {
        link.addListener(this::onMessage);
        link.startHeartbeat(5_000);
        expectOk(link.request(new ClientMessage.Hello(UUID.randomUUID(), nickname)));
        expectOk(link.request(new ClientMessage.JoinRoom(UUID.randomUUID(), roomId)));
        expectOk(link.request(new ClientMessage.ClaimSeat(UUID.randomUUID(), roomId, colour)));
        log.accept(nickname + " seated as " + colour + " in " + roomId);
        link.whenClosed().thenRun(() -> gameOver.completeExceptionally(new java.io.IOException("connection closed")));
        try {
            List<Colour> standings = gameOver.get(timeoutSeconds, TimeUnit.SECONDS);
            return new Result(rolls, moves, standings);
        } catch (TimeoutException e) {
            throw new TimeoutException("Game did not finish within " + timeoutSeconds + " s");
        } finally {
            actions.shutdownNow();
        }
    }

    private void expectOk(CompletableFuture<Response> future) throws Exception {
        Response response = future.get(SETUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!response.isOk()) {
            throw new RefusedException(response.errorCode(), response.message());
        }
    }

    // runs on the link's reader thread: never sleep or block here, hand the turn to the action thread
    private void onMessage(ServerMessage message) {
        if (message instanceof ServerMessage.YourTurn turn) {
            actions.schedule(() -> act(turn), delayMs, TimeUnit.MILLISECONDS);
        } else if (message instanceof ServerMessage.GameOver over) {
            gameOver.complete(over.standings());
        }
    }

    private void act(ServerMessage.YourTurn turn) {
        if (turn.phase() != Phase.AWAITING_MOVE) { // a roll, or the roll for the playing order
            rolls++;
            link.request(new ClientMessage.Roll(UUID.randomUUID(), roomId, turn.turnNumber()));
        } else {
            moves++;
            int choice = strategy.choose(turn.options());
            log.accept(colour + " rolled " + turn.roll() + ": " + turn.options().get(choice).description());
            link.request(new ClientMessage.ChooseMove(UUID.randomUUID(), roomId, turn.turnNumber(), choice));
        }
    }
}
