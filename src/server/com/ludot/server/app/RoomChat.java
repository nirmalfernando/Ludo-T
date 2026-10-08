package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Role;
import com.ludot.protocol.ServerMessage.ChatMessage;
import java.util.Map;

final class RoomChat {

    private final RoomState state;
    private final RoomGuards guards;
    private final Broadcaster broadcaster;
    private final GameRepository repository;

    RoomChat(RoomState state, RoomGuards guards, Broadcaster broadcaster, GameRepository repository) {
        this.state = state;
        this.guards = guards;
        this.broadcaster = broadcaster;
        this.repository = repository;
    }

    void register(Map<Class<?>, RequestHandler> handlers) {
        handlers.put(ClientMessage.SendChat.class, (from, request) -> sendChat(from, (ClientMessage.SendChat) request));
    }

    private Reply sendChat(Participant from, ClientMessage.SendChat request) {
        Member member = guards.requireMember(from);
        String text = state.chat.clean(request.text());
        state.chat.admit(member.id());
        Role role = state.roleOf(member.id());
        Colour colour = state.seats.seatOf(member.id()).map(SeatTable.Seat::colour).orElse(null);
        return Reply.ok(request).then(() -> publish(member.id(), member.nickname(), role, colour, text, false));
    }

    void postSystem(String text) {
        publish(null, "system", null, null, text, true);
    }

    private void publish(String senderId, String nickname, Role role, Colour colour, String text, boolean system) {
        ChatMessage message = broadcaster.broadcast(s -> new ChatMessage(state.roomId, s, senderId, nickname, role,
                colour, text, state.chat.now(), system));
        if (message != null) {
            state.chat.remember(message);
            repository.record(new ChatRecord(state.roomId, message.seq(), senderId, text, system, message.sentAt()));
        }
    }
}
