package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.MemberDto;
import com.ludot.protocol.Phase;
import com.ludot.protocol.Role;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import com.ludot.protocol.ServerMessage.Response;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import com.ludot.server.concurrent.NamedThreadFactory;
import com.ludot.server.game.RoomGame;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One room, run as an actor: a private single-thread executor with a bounded queue processes every
 * command for the room one at a time, in arrival order. Requests from connection threads, timers and
 * AI actions all enter as queued commands, so all room state (members, seats, game, chat, sequence
 * numbers) is confined to that one thread and needs no locks. The queue is the "put requests in a queue
 * for processing as soon as possible" the assignment asks for; when it is full the sender gets SERVER_BUSY.
 *
 * <p>Every response is sent before the pushes it caused, and every push to the room carries the next
 * sequence number, so each member sees the same totally ordered stream, chat included.
 */
public final class GameSession {

    private static final Logger LOG = Logger.getLogger(GameSession.class.getName());
    private static final int REQUEST_MEMORY = 64;
    private static final long RETRY_DELAY_MS = 20;

    private interface Handler {
        Reply handle(Participant from, ClientMessage.Request request);
    }

    /** A response plus the work to do after it has been sent. */
    private record Reply(Response response, Runnable followUp) {

        static Reply ok(ClientMessage.Request request) {
            return new Reply(Response.ok(request.requestId()), null);
        }

        static Reply ok(ClientMessage.Request request, Serializable payload) {
            return new Reply(Response.ok(request.requestId(), payload), null);
        }

        Reply then(Runnable work) {
            return new Reply(response, work);
        }
    }

    private static final class Member {
        private final PlayerIdentity identity;
        private ClientSink sink;

        Member(PlayerIdentity identity, ClientSink sink) {
            this.identity = identity;
            this.sink = sink;
        }

        String id() {
            return identity.id();
        }
    }

    // fixed for the room's life; safe to read from any thread
    private final String roomId;
    private final RoomMode mode;
    private final Variant variant;
    private final long seed;
    private final SessionConfig config;
    private final ScheduledExecutorService scheduler;
    private final LobbyPublisher lobby;
    private final GameRepository repository;
    private final ThreadPoolExecutor executor;
    private final Map<Class<?>, Handler> handlers = new HashMap<>();

    // confined to the room thread
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final SeatTable seats = new SeatTable();
    private final RoomGame game;
    private final ChatChannel chat;
    private final RequestLog requests = new RequestLog(REQUEST_MEMORY);
    private final Deque<String> log = new ArrayDeque<>();
    private final List<Member> slowConsumers = new ArrayList<>();
    private RoomStatus status = RoomStatus.WAITING;
    private Speed speed = Speed.NORMAL;
    private String hostId;
    private long seq;
    private boolean autoPaused;
    private ScheduledFuture<?> pendingAi;
    private ScheduledFuture<?> closeTimer;

    private volatile RoomSummary summary;

    public GameSession(String roomId, RoomMode mode, Variant variant, Long seed, SessionConfig config,
                       ScheduledExecutorService scheduler, LobbyPublisher lobby, GameRepository repository,
                       LongSupplier clock) {
        this.roomId = roomId;
        this.mode = mode;
        this.variant = variant;
        this.seed = seed != null ? seed : ThreadLocalRandom.current().nextLong();
        this.config = config;
        this.scheduler = scheduler;
        this.lobby = lobby;
        this.repository = repository;
        this.game = new RoomGame(variant, this.seed);
        this.chat = new ChatChannel(config, clock);
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(config.queueCapacity()),
                new NamedThreadFactory("room-" + roomId, false), new ThreadPoolExecutor.AbortPolicy());
        this.summary = buildSummary();
        registerHandlers();
    }

    // ------------------------------------------------------------------ entry points (any thread)

    public String roomId() {
        return roomId;
    }

    /** The latest summary published by the room thread. */
    public RoomSummary summary() {
        return summary;
    }

    /** Commands waiting for the room thread; logged by the server to show queueing. */
    public int queueDepth() {
        return executor.getQueue().size();
    }

    /**
     * Queues a request from a connection thread. If the room's queue is full the sender is told
     * SERVER_BUSY immediately instead of letting memory grow without limit.
     */
    public void submit(Participant from, ClientMessage.Request request) {
        try {
            executor.execute(() -> run(() -> handle(from, request)));
        } catch (RejectedExecutionException e) {
            ErrorCode code = executor.isShutdown() ? ErrorCode.UNKNOWN_ROOM : ErrorCode.SERVER_BUSY;
            from.sink().offer(Response.error(request.requestId(), code, "Room cannot take requests right now"));
        }
    }

    /** Makes the creator the first member and host; a Bot match starts straight away. */
    public void attachCreator(Participant creator) {
        submitInternal(() -> {
            members.put(creator.id(), new Member(creator.identity(), creator.sink()));
            hostId = creator.id();
            creator.sink().offer(snapshot());
            broadcastMembers();
            if (mode == RoomMode.BOT_MATCH) {
                beginGame();
            }
            publishSummary();
        });
    }

    /** The connection dropped (not a polite leave); a seat held by that person is handed to the AI. */
    public void connectionLost(String playerId, ClientSink sink) {
        submitInternal(() -> {
            Member member = members.get(playerId);
            if (member != null && member.sink == sink) {
                removeMember(member);
            }
        });
    }

    /** Stops the room: no more commands are accepted and timers are cancelled. */
    public void shutdown() {
        cancelFuture(pendingAi);
        cancelFuture(closeTimer);
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------ command plumbing (room thread)

    private void registerHandlers() {
        handlers.put(ClientMessage.JoinRoom.class, (p, r) -> joinRoom(p, r));
        handlers.put(ClientMessage.LeaveRoom.class, (p, r) -> leaveRoom(p, r));
        handlers.put(ClientMessage.ClaimSeat.class, (p, r) -> claimSeat(p, (ClientMessage.ClaimSeat) r));
        handlers.put(ClientMessage.ReleaseSeat.class, (p, r) -> releaseSeat(p, r));
        handlers.put(ClientMessage.ReclaimSeat.class, (p, r) -> reclaimSeat(p, r));
        handlers.put(ClientMessage.StartGame.class, (p, r) -> startGame(p, r));
        handlers.put(ClientMessage.Roll.class, (p, r) -> roll(p, (ClientMessage.Roll) r));
        handlers.put(ClientMessage.ChooseMove.class, (p, r) -> chooseMove(p, (ClientMessage.ChooseMove) r));
        handlers.put(ClientMessage.PauseGame.class, (p, r) -> pauseGame(p, r));
        handlers.put(ClientMessage.ResumeGame.class, (p, r) -> resumeGame(p, r));
        handlers.put(ClientMessage.SetSpeed.class, (p, r) -> setSpeed(p, (ClientMessage.SetSpeed) r));
        handlers.put(ClientMessage.RequestSnapshot.class, (p, r) -> requestSnapshot(p, r));
        handlers.put(ClientMessage.SendChat.class, (p, r) -> sendChat(p, (ClientMessage.SendChat) r));
    }

    private void run(Runnable command) {
        try {
            command.run();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected failure in room " + roomId, e);
        } finally {
            reapSlowConsumers();
        }
    }

    // best effort for timers and AI: if the queue is full, try again shortly rather than stall the game
    private void submitInternal(Runnable command) {
        try {
            executor.execute(() -> run(command));
        } catch (RejectedExecutionException e) {
            if (!executor.isShutdown()) {
                schedule(() -> submitInternal(command), RETRY_DELAY_MS);
            }
        }
    }

    private void handle(Participant from, ClientMessage.Request request) {
        Response earlier = requests.find(from.id(), request.requestId());
        if (earlier != null) {
            from.sink().offer(earlier);
            return;
        }
        Reply reply;
        try {
            reply = handlers.get(request.getClass()).handle(from, request);
        } catch (RequestRejected rejected) {
            reply = new Reply(Response.error(request.requestId(), rejected.code(), rejected.getMessage()), null);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Request failed in room " + roomId + ": " + request, e);
            reply = new Reply(Response.error(request.requestId(), ErrorCode.INTERNAL_ERROR, "Internal error"), null);
        }
        requests.remember(from.id(), reply.response());
        from.sink().offer(reply.response());
        if (reply.followUp() != null) {
            reply.followUp().run();
        }
    }

    private static RequestRejected reject(ErrorCode code, String message) {
        return new RequestRejected(code, message);
    }

    private Member requireMember(Participant from) {
        Member member = members.get(from.id());
        if (member == null) {
            throw reject(ErrorCode.NOT_IN_ROOM, "You are not in room " + roomId);
        }
        return member;
    }

    private Member requireHost(Participant from) {
        Member member = requireMember(from);
        if (!member.id().equals(hostId)) {
            throw reject(ErrorCode.NOT_HOST, "Only the host can do that");
        }
        return member;
    }

    // ------------------------------------------------------------------ membership

    private Reply joinRoom(Participant from, ClientMessage.Request request) {
        if (status == RoomStatus.CLOSED) {
            throw reject(ErrorCode.UNKNOWN_ROOM, "Room is closed");
        }
        Member existing = members.get(from.id());
        boolean newcomer = existing == null;
        if (newcomer) {
            members.put(from.id(), new Member(from.identity(), from.sink()));
        } else {
            existing.sink = from.sink();
        }
        cancelFuture(closeTimer);
        return Reply.ok(request).then(() -> {
            // snapshot and membership change in the same step: later pushes carry seq > the snapshot's
            from.sink().offer(snapshot());
            if (newcomer) {
                broadcastMembers();
                postSystem(from.identity().nickname() + " joined as a spectator");
            }
            resumeIfAutoPaused();
            publishSummary();
        });
    }

    private Reply leaveRoom(Participant from, ClientMessage.Request request) {
        Member member = requireMember(from);
        return Reply.ok(request).then(() -> removeMember(member));
    }

    private void removeMember(Member member) {
        members.remove(member.id());
        boolean seatChanged = detachSeat(member.id());
        boolean hostChanged = member.id().equals(hostId) && reassignHost();
        if (seatChanged) {
            broadcastSeats();
        }
        broadcastMembers();
        if (hostChanged) {
            broadcastRoomState();
        }
        postSystem(member.identity.nickname() + " left");
        publishSummary();
        if (members.isEmpty()) {
            onEmpty();
        } else if (seatChanged) {
            scheduleNext();
        }
    }

    // before the game a leaver's seat is freed; during it the seat stays theirs but the AI plays it
    private boolean detachSeat(String playerId) {
        return seats.seatOf(playerId).map(seat -> {
            if (status == RoomStatus.WAITING) {
                seats.releaseHeldBy(playerId);
            } else {
                seats.setController(seat.colour(), Controller.DISCONNECTED);
            }
            return true;
        }).orElse(false);
    }

    private boolean reassignHost() {
        hostId = members.isEmpty() ? null : members.keySet().iterator().next();
        return hostId != null;
    }

    private void onEmpty() {
        if (mode == RoomMode.BOT_MATCH && status == RoomStatus.RUNNING) {
            status = RoomStatus.PAUSED;
            autoPaused = true;
            cancelFuture(pendingAi);
        }
        cancelFuture(closeTimer);
        closeTimer = schedule(() -> submitInternal(this::closeIfStillEmpty), config.emptyRoomCloseMs());
    }

    private void closeIfStillEmpty() {
        if (members.isEmpty()) {
            status = RoomStatus.CLOSED;
            cancelFuture(pendingAi);
            summary = buildSummary();
            lobby.roomClosed(roomId);
            executor.shutdown();
        }
    }

    private void resumeIfAutoPaused() {
        if (autoPaused && !members.isEmpty()) {
            autoPaused = false;
            status = RoomStatus.RUNNING;
            broadcastRoomState();
            scheduleNext();
        }
    }

    // ------------------------------------------------------------------ seats

    private Reply claimSeat(Participant from, ClientMessage.ClaimSeat request) {
        requireMember(from);
        requireSeatsOpen();
        seats.claim(from.identity(), request.colour());
        return Reply.ok(request).then(() -> {
            broadcastSeats();
            broadcastMembers();
            postSystem(from.identity().nickname() + " took " + request.colour());
            publishSummary();
        });
    }

    private Reply releaseSeat(Participant from, ClientMessage.Request request) {
        requireMember(from);
        requireSeatsOpen();
        if (!seats.releaseHeldBy(from.id())) {
            throw reject(ErrorCode.NOT_A_PLAYER, "You do not hold a seat");
        }
        return Reply.ok(request).then(() -> {
            broadcastSeats();
            broadcastMembers();
            publishSummary();
        });
    }

    private void requireSeatsOpen() {
        if (mode != RoomMode.PLAY || status != RoomStatus.WAITING) {
            throw reject(ErrorCode.SEATS_LOCKED, "Seats can only change before a Play game starts");
        }
    }

    private Reply reclaimSeat(Participant from, ClientMessage.Request request) {
        requireMember(from);
        SeatTable.Seat seat = seats.seatOf(from.id()).orElseThrow(
                () -> reject(ErrorCode.NOT_A_PLAYER, "You do not hold a seat"));
        if (seat.controller() != Controller.AFK && seat.controller() != Controller.DISCONNECTED) {
            throw reject(ErrorCode.NOT_A_PLAYER, "Your seat is not waiting to be reclaimed");
        }
        seats.setController(seat.colour(), Controller.HUMAN);
        return Reply.ok(request).then(() -> {
            broadcastSeats();
            scheduleNext();
        });
    }

    // ------------------------------------------------------------------ game flow

    private Reply startGame(Participant from, ClientMessage.Request request) {
        requireHost(from);
        if (mode != RoomMode.PLAY || status != RoomStatus.WAITING) {
            throw reject(ErrorCode.SEATS_LOCKED, "The game has already started");
        }
        if (seats.humanSeats() == 0) {
            throw reject(ErrorCode.NO_HUMAN_SEATS, "At least one seat must be taken by a person");
        }
        return Reply.ok(request).then(() -> {
            beginGame();
            publishSummary();
        });
    }

    private void beginGame() {
        seats.fillFreeSeatsWithAi();
        status = RoomStatus.RUNNING;
        RoomGame.Step step = game.start();
        broadcastSeats();
        broadcastRoomState();
        postSystem("The game has started");
        afterStep(step);
    }

    private Reply roll(Participant from, ClientMessage.Roll request) {
        requireTurn(from, request.turnNumber(), Phase.AWAITING_ROLL);
        Colour colour = game.currentColour();
        RoomGame.Step step = game.roll(colour);
        record(request.turnNumber(), colour, step.rolled(), null, DecisionSource.HUMAN);
        return Reply.ok(request, step.rolled()).then(() -> afterStep(step));
    }

    private Reply chooseMove(Participant from, ClientMessage.ChooseMove request) {
        requireTurn(from, request.turnNumber(), Phase.AWAITING_MOVE);
        int index = request.optionIndex();
        if (index < 0 || index >= game.options().size()) {
            throw reject(ErrorCode.INVALID_MOVE, "No such move: " + index);
        }
        Colour colour = game.currentColour();
        Integer rolled = game.lastRoll();
        RoomGame.Step step = game.applyMove(colour, index);
        record(request.turnNumber(), colour, rolled, index, DecisionSource.HUMAN);
        return Reply.ok(request).then(() -> afterStep(step));
    }

    // validation order: member, game state, whose turn, which turn, which phase
    private void requireTurn(Participant from, int turnNumber, Phase expected) {
        Member member = requireMember(from);
        requireRunning();
        SeatTable.Seat active = seats.seat(game.currentColour());
        if (seats.seatOf(member.id()).isEmpty()) {
            throw reject(ErrorCode.NOT_A_PLAYER, "Spectators cannot play");
        }
        if (active.controller() != Controller.HUMAN || !active.heldBy(member.id())) {
            throw reject(ErrorCode.NOT_YOUR_TURN, "It is " + active.colour() + "'s turn");
        }
        if (turnNumber != game.turnNumber()) {
            throw reject(ErrorCode.STALE_TURN, "Turn " + turnNumber + " is over; now at " + game.turnNumber());
        }
        if (game.phase() != expected) {
            throw reject(ErrorCode.WRONG_PHASE, "Expected " + game.phase());
        }
    }

    private void requireRunning() {
        switch (status) {
            case RUNNING -> { }
            case WAITING -> throw reject(ErrorCode.GAME_NOT_RUNNING, "The game has not started");
            case PAUSED -> throw reject(ErrorCode.GAME_PAUSED, "The game is paused");
            case FINISHED -> throw reject(ErrorCode.GAME_FINISHED, "The game is over");
            default -> throw reject(ErrorCode.UNKNOWN_ROOM, "Room is closed");
        }
    }

    private void afterStep(RoomGame.Step step) {
        broadcast(s -> new ServerMessage.StateUpdate(roomId, s, game.turnInfo(null), step.events(),
                game.board(), game.stateHash()));
        remember(step);
        if (game.isFinished()) {
            finishGame();
        } else {
            scheduleNext();
        }
    }

    private void finishGame() {
        status = RoomStatus.FINISHED;
        cancelFuture(pendingAi);
        List<Colour> standings = game.standings();
        broadcastRoomState();
        broadcast(s -> new ServerMessage.GameOver(roomId, s, standings, roomId));
        postSystem(standings.get(0) + " wins!");
        publishSummary();
    }

    private void remember(RoomGame.Step step) {
        step.events().stream().map(e -> e.text()).filter(t -> !t.isBlank()).forEach(log::addLast);
        while (log.size() > config.recentLogSize()) {
            log.removeFirst();
        }
    }

    // ------------------------------------------------------------------ AI and pacing

    /** Decides what happens next: tell a human it is their turn, or schedule the AI to act after the pacing delay. */
    private void scheduleNext() {
        cancelFuture(pendingAi);
        if (status != RoomStatus.RUNNING || game.isFinished()) {
            return;
        }
        SeatTable.Seat seat = seats.seat(game.currentColour());
        if (seat.controller() == Controller.HUMAN && !nothingToDecide()) {
            notifyHumanTurn(seat);
            return;
        }
        int expectedTurn = game.turnNumber();
        pendingAi = schedule(() -> submitInternal(() -> aiAct(expectedTurn)), config.delayMs(speed));
    }

    private boolean nothingToDecide() {
        return game.phase() == Phase.AWAITING_MOVE && game.onlySkipAvailable();
    }

    private void notifyHumanTurn(SeatTable.Seat seat) {
        Member member = members.get(seat.playerId());
        if (member == null) {
            return;
        }
        deliver(member, new ServerMessage.YourTurn(roomId, game.turnNumber(), game.phase(), game.lastRoll(),
                game.options(), null));
    }

    // runs on the room thread; a stale or paused action does nothing, a started one always completes
    private void aiAct(int expectedTurn) {
        if (status != RoomStatus.RUNNING || game.isFinished() || game.turnNumber() != expectedTurn) {
            return;
        }
        Colour colour = game.currentColour();
        SeatTable.Seat seat = seats.seat(colour);
        boolean forcedSkip = seat.controller() == Controller.HUMAN && nothingToDecide();
        if (seat.controller() == Controller.HUMAN && !forcedSkip) {
            return;
        }
        DecisionSource source = forcedSkip ? DecisionSource.AUTO_SKIP : DecisionSource.AI;
        RoomGame.Step step;
        if (game.phase() == Phase.AWAITING_ROLL) {
            step = game.roll(colour);
            record(expectedTurn, colour, step.rolled(), null, source);
        } else {
            int choice = forcedSkip ? 0 : game.strategyChoice();
            Integer rolled = game.lastRoll();
            step = game.applyMove(colour, choice);
            record(expectedTurn, colour, rolled, choice, source);
        }
        afterStep(step);
    }

    private void record(int turnNumber, Colour colour, Integer roll, Integer optionIndex, DecisionSource source) {
        repository.recordDecision(new DecisionRecord(roomId, turnNumber, colour, roll, optionIndex, source));
    }

    // ------------------------------------------------------------------ host controls

    private Reply pauseGame(Participant from, ClientMessage.Request request) {
        requireHost(from);
        requireRunning();
        status = RoomStatus.PAUSED;
        cancelFuture(pendingAi);
        return Reply.ok(request).then(() -> {
            broadcastRoomState();
            postSystem("The host paused the game");
            publishSummary();
        });
    }

    private Reply resumeGame(Participant from, ClientMessage.Request request) {
        requireHost(from);
        if (status != RoomStatus.PAUSED) {
            requireRunning();
            throw reject(ErrorCode.GAME_NOT_RUNNING, "The game is not paused");
        }
        status = RoomStatus.RUNNING;
        autoPaused = false;
        return Reply.ok(request).then(() -> {
            broadcastRoomState();
            postSystem("The host resumed the game");
            scheduleNext();
            publishSummary();
        });
    }

    private Reply setSpeed(Participant from, ClientMessage.SetSpeed request) {
        requireHost(from);
        speed = request.speed();
        return Reply.ok(request).then(() -> {
            broadcastRoomState();
            scheduleNext();
        });
    }

    private Reply requestSnapshot(Participant from, ClientMessage.Request request) {
        Member member = requireMember(from);
        return Reply.ok(request).then(() -> deliver(member, snapshot()));
    }

    // ------------------------------------------------------------------ chat

    private Reply sendChat(Participant from, ClientMessage.SendChat request) {
        Member member = requireMember(from);
        String text = chat.clean(request.text());
        chat.admit(member.id());
        Role role = seats.seatOf(member.id()).isPresent() ? Role.PLAYER : Role.SPECTATOR;
        Colour colour = seats.seatOf(member.id()).map(SeatTable.Seat::colour).orElse(null);
        return Reply.ok(request).then(() -> publishChat(member.id(), member.identity.nickname(), role, colour, text, false));
    }

    private void postSystem(String text) {
        publishChat(null, "system", null, null, text, true);
    }

    private void publishChat(String senderId, String nickname, Role role, Colour colour, String text, boolean system) {
        ChatMessage message = broadcast(s -> new ChatMessage(roomId, s, senderId, nickname, role, colour, text,
                chat.now(), system));
        if (message != null) {
            chat.remember(message);
            repository.recordChat(new ChatRecord(roomId, message.seq(), senderId, text, system, message.sentAt()));
        }
    }

    // ------------------------------------------------------------------ pushes

    /**
     * Gives the push the next sequence number and offers it to every member. Returns null (and uses no
     * number) when nobody is in the room. A member whose queue is full is noted and dropped afterwards.
     */
    private <T extends ServerMessage.RoomPush> T broadcast(LongFunction<T> factory) {
        if (members.isEmpty()) {
            return null;
        }
        T message = factory.apply(++seq);
        for (Member member : members.values()) {
            deliver(member, message);
        }
        return message;
    }

    private void deliver(Member member, ServerMessage message) {
        if (!member.sink.offer(message)) {
            member.sink.close();
            slowConsumers.add(member);
        }
    }

    private void reapSlowConsumers() {
        while (!slowConsumers.isEmpty()) {
            List<Member> dead = new ArrayList<>(slowConsumers);
            slowConsumers.clear();
            for (Member member : dead) {
                if (members.containsKey(member.id())) {
                    removeMember(member);
                }
            }
        }
    }

    private void broadcastSeats() {
        broadcast(s -> new ServerMessage.SeatsUpdated(roomId, s, seats.dtos()));
    }

    private void broadcastMembers() {
        broadcast(s -> new ServerMessage.MembersChanged(roomId, s, memberDtos()));
    }

    private void broadcastRoomState() {
        broadcast(s -> new ServerMessage.RoomStateChanged(roomId, s, status, status == RoomStatus.PAUSED,
                speed, hostId));
    }

    private ServerMessage.RoomSnapshot snapshot() {
        return new ServerMessage.RoomSnapshot(roomId, seq, mode, variant, seed, status, hostId,
                status == RoomStatus.PAUSED, speed, seats.dtos(), game.board(), game.turnInfo(null),
                memberDtos(), List.copyOf(log), chat.recent(), game.stateHash());
    }

    private List<MemberDto> memberDtos() {
        List<MemberDto> dtos = new ArrayList<>();
        for (Member member : members.values()) {
            Role role = seats.seatOf(member.id()).isPresent() ? Role.PLAYER : Role.SPECTATOR;
            dtos.add(new MemberDto(member.id(), member.identity.nickname(), role, member.id().equals(hostId)));
        }
        return dtos;
    }

    private void publishSummary() {
        summary = buildSummary();
        lobby.roomChanged(summary);
    }

    private RoomSummary buildSummary() {
        long spectators = members.values().stream().filter(m -> seats.seatOf(m.id()).isEmpty()).count();
        Member host = hostId == null ? null : members.get(hostId);
        return new RoomSummary(roomId, mode, variant, status, seats.humanSeats(), seats.count(Controller.AI),
                (int) spectators, host == null ? "" : host.identity.nickname());
    }

    // a stopped scheduler means the server is shutting down: there is nothing left to schedule
    private ScheduledFuture<?> schedule(Runnable task, long delayMs) {
        try {
            return scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            return null;
        }
    }

    private static void cancelFuture(ScheduledFuture<?> future) {
        if (future != null) {
            future.cancel(false);
        }
    }
}
