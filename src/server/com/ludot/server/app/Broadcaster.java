package com.ludot.server.app;

import com.ludot.protocol.Controller;
import com.ludot.protocol.MemberDto;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.ServerMessage;
import com.ludot.server.game.RoomGame;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

final class Broadcaster {

    private final RoomState state;
    private final LobbyPublisher lobby;
    private final List<Member> slowConsumers = new ArrayList<>();
    private long seq;

    Broadcaster(RoomState state, LobbyPublisher lobby) {
        this.state = state;
        this.lobby = lobby;
    }

    <T extends ServerMessage.RoomPush> T broadcast(LongFunction<T> factory) {
        if (state.members.isEmpty()) {
            return null;
        }
        T message = factory.apply(++seq);
        for (Member member : state.members.values()) {
            deliver(member, message);
        }
        return message;
    }

    void deliver(Member member, ServerMessage message) {
        if (!member.sink().offer(message)) {
            member.sink().close();
            slowConsumers.add(member);
        }
    }

    List<Member> takeSlowConsumers() {
        List<Member> dead = new ArrayList<>(slowConsumers);
        slowConsumers.clear();
        return dead;
    }

    void seats() {
        broadcast(s -> new ServerMessage.SeatsUpdated(state.roomId, s, state.seats.dtos()));
    }

    void members() {
        broadcast(s -> new ServerMessage.MembersChanged(state.roomId, s, memberDtos()));
    }

    void roomState() {
        broadcast(s -> new ServerMessage.RoomStateChanged(state.roomId, s, state.status,
                state.status == RoomStatus.PAUSED, state.speed, state.hostId));
    }

    void stateUpdate(RoomGame.Step step) {
        broadcast(s -> new ServerMessage.StateUpdate(state.roomId, s, state.game.turnInfo(state.deadline),
                step.events(), state.game.board(), state.game.stateHash()));
    }

    void turnRefresh() {
        if (state.deadline != null) {
            broadcast(s -> new ServerMessage.StateUpdate(state.roomId, s, state.game.turnInfo(state.deadline),
                    List.of(), state.game.board(), state.game.stateHash()));
        }
    }

    ServerMessage.RoomSnapshot snapshot() {
        return new ServerMessage.RoomSnapshot(state.roomId, seq, state.mode, state.variant, state.seed, state.status,
                state.hostId, state.status == RoomStatus.PAUSED, state.speed, state.seats.dtos(), state.game.board(),
                state.game.turnInfo(state.deadline), memberDtos(), List.copyOf(state.log), state.chat.recent(),
                state.game.stateHash(), state.game.playOrder());
    }

    private List<MemberDto> memberDtos() {
        List<MemberDto> dtos = new ArrayList<>();
        for (Member member : state.members.values()) {
            dtos.add(new MemberDto(member.id(), member.nickname(), state.roleOf(member.id()),
                    member.id().equals(state.hostId)));
        }
        return dtos;
    }

    void publishSummary() {
        state.summary = buildSummary();
        lobby.roomChanged(state.summary);
    }

    void publishClosed() {
        state.summary = buildSummary();
        lobby.roomClosed(state.roomId);
    }

    RoomSummary buildSummary() {
        long spectators = state.members.values().stream().filter(m -> state.seats.seatOf(m.id()).isEmpty()).count();
        Member host = state.host();
        return new RoomSummary(state.roomId, state.mode, state.variant, state.status, state.seats.humanSeats(),
                state.seats.count(Controller.AI), (int) spectators,
                host == null ? "" : host.nickname());
    }
}
