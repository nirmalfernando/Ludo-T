package com.ludot.protocol;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Every message a client may send. Records are immutable and validate themselves on construction,
 * which also runs when the server deserialises them, so a malformed message never reaches the game.
 */
public sealed interface ClientMessage extends Serializable permits ClientMessage.Ping, ClientMessage.Request {

    /** Liveness probe; answered with a Pong and never with a Response. */
    record Ping() implements ClientMessage {
    }

    /** A message that expects a Response; the id lets the server detect duplicates. */
    sealed interface Request extends ClientMessage permits Hello, Reconnect, SignOut, ListRooms, CreateRoom, SendChat, ListFinishedGames, StartReplay, ControlReplay, RoomRequest {
        UUID requestId();
    }

    /** A request addressed to one room, so the router can hand it to that room's queue. */
    sealed interface RoomRequest extends Request permits JoinRoom, LeaveRoom, ClaimSeat, ReleaseSeat, ReclaimSeat, StartGame, Roll, ChooseMove, PauseGame, ResumeGame, SetSpeed, RequestSnapshot {
        String roomId();
    }

    /**
     * Introduces a person. {@code reconnectable} says this client can come back with its token after a dropped
     * connection (the server then keeps the identity for a grace period); automatic test clients leave it false.
     */
    record Hello(UUID requestId, String nickname, boolean reconnectable) implements Request {
        public Hello {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(nickname, "nickname");
        }

        public Hello(UUID requestId, String nickname) {
            this(requestId, nickname, false);
        }
    }

    record Reconnect(UUID requestId, String token) implements Request {
        public Reconnect {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(token, "token");
        }
    }

    /** A polite goodbye: the nickname is freed at once instead of being held for a reconnect. */
    record SignOut(UUID requestId) implements Request {
        public SignOut {
            Objects.requireNonNull(requestId, "requestId");
        }
    }

    record ListRooms(UUID requestId) implements Request {
        public ListRooms {
            Objects.requireNonNull(requestId, "requestId");
        }
    }

    record CreateRoom(UUID requestId, RoomMode mode, Variant variant, Long seed) implements Request {
        public CreateRoom {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(variant, "variant");
        }
    }

    record SendChat(UUID requestId, String roomId, String text) implements Request {
        public SendChat {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(text, "text");
        }
    }

    record ListFinishedGames(UUID requestId, int page) implements Request {
        public ListFinishedGames {
            Objects.requireNonNull(requestId, "requestId");
        }
    }

    record StartReplay(UUID requestId, String gameId, Speed speed) implements Request {
        public StartReplay {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(gameId, "gameId");
            Objects.requireNonNull(speed, "speed");
        }
    }

    record ControlReplay(UUID requestId, String replayId, ReplayAction action, Speed speed) implements Request {
        public ControlReplay {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(replayId, "replayId");
            Objects.requireNonNull(action, "action");
        }
    }

    record JoinRoom(UUID requestId, String roomId) implements RoomRequest {
        public JoinRoom {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record LeaveRoom(UUID requestId, String roomId) implements RoomRequest {
        public LeaveRoom {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record ClaimSeat(UUID requestId, String roomId, Colour colour) implements RoomRequest {
        public ClaimSeat {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
            Objects.requireNonNull(colour, "colour");
        }
    }

    record ReleaseSeat(UUID requestId, String roomId) implements RoomRequest {
        public ReleaseSeat {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record ReclaimSeat(UUID requestId, String roomId) implements RoomRequest {
        public ReclaimSeat {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record StartGame(UUID requestId, String roomId) implements RoomRequest {
        public StartGame {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record Roll(UUID requestId, String roomId, int turnNumber) implements RoomRequest {
        public Roll {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record ChooseMove(UUID requestId, String roomId, int turnNumber, int optionIndex) implements RoomRequest {
        public ChooseMove {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record PauseGame(UUID requestId, String roomId) implements RoomRequest {
        public PauseGame {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record ResumeGame(UUID requestId, String roomId) implements RoomRequest {
        public ResumeGame {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }

    record SetSpeed(UUID requestId, String roomId, Speed speed) implements RoomRequest {
        public SetSpeed {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
            Objects.requireNonNull(speed, "speed");
        }
    }

    record RequestSnapshot(UUID requestId, String roomId) implements RoomRequest {
        public RequestSnapshot {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(roomId, "roomId");
        }
    }
}
