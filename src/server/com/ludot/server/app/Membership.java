package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

final class Membership {

    private final RoomState state;
    private final RoomGuards guards;
    private final Broadcaster broadcaster;
    private final RoomChat chat;
    private final TurnDriver turns;
    private final RoomActor actor;
    private final GameRepository repository;
    private final Runnable closeRoom;
    private volatile ScheduledFuture<?> closeTimer;

    Membership(RoomState state, RoomGuards guards, Broadcaster broadcaster, RoomChat chat, TurnDriver turns,
               RoomActor actor, GameRepository repository, Runnable closeRoom) {
        this.state = state;
        this.guards = guards;
        this.broadcaster = broadcaster;
        this.chat = chat;
        this.turns = turns;
        this.actor = actor;
        this.repository = repository;
        this.closeRoom = closeRoom;
    }

    void register(Map<Class<?>, RequestHandler> handlers) {
        handlers.put(ClientMessage.JoinRoom.class, this::join);
        handlers.put(ClientMessage.LeaveRoom.class, this::leave);
        handlers.put(ClientMessage.RequestSnapshot.class, this::requestSnapshot);
    }

    void cancelCloseTimer() {
        RoomActor.cancel(closeTimer);
    }

    // ------------------------------------------------------------------ joining

    void addCreator(Participant creator) {
        state.members.put(creator.id(), new Member(creator.identity(), creator.sink()));
        state.hostId = creator.id();
        repository.record(new RoomRecord(state.roomId, state.mode, state.variant, state.seed, creator.id()));
        creator.sink().offer(broadcaster.snapshot());
        broadcaster.members();
    }

    private Reply join(Participant from, ClientMessage.Request request) {
        if (state.status == RoomStatus.CLOSED) {
            throw RoomGuards.reject(ErrorCode.UNKNOWN_ROOM, "Room is closed");
        }
        Member existing = state.members.get(from.id());
        boolean newcomer = existing == null;
        boolean returning = newcomer && reclaimDroppedSeat(from);
        if (newcomer) {
            state.members.put(from.id(), new Member(from.identity(), from.sink()));
            if (state.hostId == null) {
                state.hostId = from.id(); // the first person into an empty room becomes its host
            }
        } else {
            existing.attach(from.sink());
        }
        cancelCloseTimer();
        return Reply.ok(request).then(() -> {
            // snapshot and membership change in the same step: later pushes carry seq > the snapshot's
            from.sink().offer(broadcaster.snapshot());
            if (returning) {
                welcomeBack(from);
            } else if (newcomer) {
                broadcaster.members();
                chat.postSystem(from.identity().nickname() + " joined as a spectator");
            }
            resumeIfAutoPaused();
            broadcaster.publishSummary();
        });
    }

    // someone whose connection dropped in the middle of a game gets their seat back as soon as they rejoin
    private boolean reclaimDroppedSeat(Participant from) {
        return state.seats.seatOf(from.id()).filter(seat -> seat.controller() == Controller.DISCONNECTED).map(seat -> {
            state.seats.setController(seat.colour(), Controller.HUMAN);
            turns.resetMisses(seat.colour());
            return true;
        }).orElse(false);
    }

    private void welcomeBack(Participant from) {
        broadcaster.seats();
        broadcaster.members();
        chat.postSystem(from.identity().nickname() + " is back");
        turns.scheduleNext();
        broadcaster.turnRefresh();
    }

    private Reply requestSnapshot(Participant from, ClientMessage.Request request) {
        Member member = guards.requireMember(from);
        return Reply.ok(request).then(() -> broadcaster.deliver(member, broadcaster.snapshot()));
    }

    // ------------------------------------------------------------------ leaving

    private Reply leave(Participant from, ClientMessage.Request request) {
        Member member = guards.requireMember(from);
        return Reply.ok(request).then(() -> remove(member));
    }

    void connectionLost(String playerId, ClientSink sink) {
        Member member = state.members.get(playerId);
        if (member != null && member.sink() == sink) {
            remove(member);
        }
    }

    void remove(Member member) {
        state.members.remove(member.id());
        boolean seatChanged = detachSeat(member.id());
        boolean hostChanged = member.id().equals(state.hostId) && reassignHost();
        if (seatChanged) {
            broadcaster.seats();
        }
        broadcaster.members();
        if (hostChanged) {
            broadcaster.roomState();
        }
        chat.postSystem(member.nickname() + " left");
        if (state.members.isEmpty()) {
            onEmpty();
        } else if (seatChanged) {
            turns.takeOverIfNeeded(); // if the leaver's turn was pending, the AI takes it over
        }
        broadcaster.publishSummary(); // after onEmpty, which may pause the game, so the lobby shows the real status
    }

    // before the game a leaver's seat is freed; during it the seat stays theirs but the AI plays it
    private boolean detachSeat(String playerId) {
        return state.seats.seatOf(playerId).map(seat -> {
            if (state.status == RoomStatus.WAITING) {
                state.seats.releaseHeldBy(playerId);
            } else {
                state.seats.setController(seat.colour(), Controller.DISCONNECTED);
            }
            return true;
        }).orElse(false);
    }

    private boolean reassignHost() {
        state.hostId = state.members.isEmpty() ? null : state.members.keySet().iterator().next();
        return state.hostId != null;
    }

    // ------------------------------------------------------------------ an empty room

    // an empty bot match pauses itself (nobody is watching); any empty room closes if it stays empty
    private void onEmpty() {
        if (state.mode == RoomMode.BOT_MATCH && state.status == RoomStatus.RUNNING) {
            state.status = RoomStatus.PAUSED;
            state.autoPaused = true;
            turns.cancelTimers();
        }
        cancelCloseTimer();
        closeTimer = actor.schedule(() -> actor.submitInternal(this::closeIfStillEmpty), state.config.emptyRoomCloseMs());
    }

    private void closeIfStillEmpty() {
        if (!state.members.isEmpty()) {
            return;
        }
        if (state.status != RoomStatus.FINISHED) {
            repository.record(new RoomStatusRecord(state.roomId, RoomStatus.CLOSED, null, state.game.turnNumber()));
        }
        state.status = RoomStatus.CLOSED;
        turns.cancelTimers();
        broadcaster.publishClosed();
        closeRoom.run();
    }

    private void resumeIfAutoPaused() {
        if (state.autoPaused && !state.members.isEmpty()) {
            state.autoPaused = false;
            state.status = RoomStatus.RUNNING;
            broadcaster.roomState();
            turns.scheduleNext();
        }
    }
}
