package com.ludot.protocol;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Every message the server may send. Messages are immutable; lists are copied on construction so one
 * instance can safely be shared by every connection's outbound queue.
 */
public sealed interface ServerMessage extends Serializable
        permits ServerMessage.Welcome, ServerMessage.Response, ServerMessage.Pong, ServerMessage.LobbyUpdate, ServerMessage.YourTurn, ServerMessage.ReplayEvent, ServerMessage.ReplayEnded, ServerMessage.RoomPush {

    /**
     * A push ordered within one room: seq increases by exactly 1 per push, so clients can detect gaps
     * and ask for a fresh snapshot. For lobby chat, roomId is null and seq is the lobby sequence.
     */
    sealed interface RoomPush extends ServerMessage permits RoomSnapshot, SeatsUpdated, StateUpdate, RoomStateChanged, MembersChanged, GameOver, ChatMessage {
        String roomId();

        long seq();
    }

    /** Reply to a Request. A duplicate request id gets the original Response again. */
    record Response(UUID requestId, ResponseStatus status, ErrorCode errorCode, String message,
                    Serializable payload) implements ServerMessage {
        public Response {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(status, "status");
        }

        public static Response ok(UUID requestId) {
            return new Response(requestId, ResponseStatus.OK, null, null, null);
        }

        public static Response ok(UUID requestId, Serializable payload) {
            return new Response(requestId, ResponseStatus.OK, null, null, payload);
        }

        public static Response error(UUID requestId, ErrorCode code, String message) {
            return new Response(requestId, ResponseStatus.ERROR, Objects.requireNonNull(code, "code"), message, null);
        }

        public boolean isOk() {
            return status == ResponseStatus.OK;
        }
    }

    record Welcome(String playerId, String nickname, String token) implements ServerMessage {
        public Welcome {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(nickname, "nickname");
            Objects.requireNonNull(token, "token");
        }
    }

    record Pong(long serverTimeMs) implements ServerMessage {
    }

    record LobbyUpdate(long lobbySeq, List<RoomSummary> rooms) implements ServerMessage {
        public LobbyUpdate {
            rooms = List.copyOf(rooms);
        }
    }

    record RoomSnapshot(String roomId, long seq, RoomMode mode, Variant variant, Long seed, RoomStatus status, String hostId, boolean paused, Speed speed, List<SeatDto> seats, BoardDto board, TurnInfo turn, List<MemberDto> members, List<String> recentLog, List<ChatMessage> recentChat, long stateHash) implements RoomPush {
        public RoomSnapshot {
            Objects.requireNonNull(roomId, "roomId");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(variant, "variant");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(hostId, "hostId");
            Objects.requireNonNull(speed, "speed");
            seats = List.copyOf(seats);
            Objects.requireNonNull(board, "board");
            members = List.copyOf(members);
            recentLog = List.copyOf(recentLog);
            recentChat = List.copyOf(recentChat);
        }
    }

    record SeatsUpdated(String roomId, long seq, List<SeatDto> seats) implements RoomPush {
        public SeatsUpdated {
            Objects.requireNonNull(roomId, "roomId");
            seats = List.copyOf(seats);
        }
    }

    record StateUpdate(String roomId, long seq, TurnInfo turn, List<EventDto> events, BoardDto board, long stateHash) implements RoomPush {
        public StateUpdate {
            Objects.requireNonNull(roomId, "roomId");
            Objects.requireNonNull(turn, "turn");
            events = List.copyOf(events);
            Objects.requireNonNull(board, "board");
        }
    }

    record YourTurn(String roomId, int turnNumber, Phase phase, Integer roll, List<MoveOption> options, Long deadlineEpochMs) implements ServerMessage {
        public YourTurn {
            Objects.requireNonNull(roomId, "roomId");
            Objects.requireNonNull(phase, "phase");
            options = List.copyOf(options);
        }
    }

    record RoomStateChanged(String roomId, long seq, RoomStatus status, boolean paused, Speed speed, String hostId) implements RoomPush {
        public RoomStateChanged {
            Objects.requireNonNull(roomId, "roomId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(speed, "speed");
            Objects.requireNonNull(hostId, "hostId");
        }
    }

    record MembersChanged(String roomId, long seq, List<MemberDto> members) implements RoomPush {
        public MembersChanged {
            Objects.requireNonNull(roomId, "roomId");
            members = List.copyOf(members);
        }
    }

    record GameOver(String roomId, long seq, List<Colour> standings, String gameId) implements RoomPush {
        public GameOver {
            Objects.requireNonNull(roomId, "roomId");
            standings = List.copyOf(standings);
        }
    }

    record ChatMessage(String roomId, long seq, String senderId, String nickname, Role role, Colour seatColour, String text, long sentAt, boolean system) implements RoomPush {
        public ChatMessage {
            Objects.requireNonNull(nickname, "nickname");
            Objects.requireNonNull(text, "text");
        }
    }

    record ReplayEvent(String replayId, int index, int total, List<EventDto> events, BoardDto board) implements ServerMessage {
        public ReplayEvent {
            Objects.requireNonNull(replayId, "replayId");
            events = List.copyOf(events);
            Objects.requireNonNull(board, "board");
        }
    }

    record ReplayEnded(String replayId, ReplayEndReason reason) implements ServerMessage {
        public ReplayEnded {
            Objects.requireNonNull(replayId, "replayId");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
