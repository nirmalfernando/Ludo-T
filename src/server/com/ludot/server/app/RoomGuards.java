package com.ludot.server.app;

import com.ludot.protocol.ErrorCode;

/** The checks several kinds of request share, each refusing with the error code the protocol specifies. */
final class RoomGuards {

    private final RoomState state;

    RoomGuards(RoomState state) {
        this.state = state;
    }

    static RequestRejected reject(ErrorCode code, String message) {
        return new RequestRejected(code, message);
    }

    Member requireMember(Participant from) {
        Member member = state.members.get(from.id());
        if (member == null) {
            throw reject(ErrorCode.NOT_IN_ROOM, "You are not in room " + state.roomId);
        }
        return member;
    }

    Member requireHost(Participant from) {
        Member member = requireMember(from);
        if (!member.id().equals(state.hostId)) {
            throw reject(ErrorCode.NOT_HOST, "Only the host can do that");
        }
        return member;
    }

    /** Gameplay is only allowed while the room is RUNNING; every other status has its own error. */
    void requireRunning() {
        switch (state.status) {
            case RUNNING -> { }
            case WAITING -> throw reject(ErrorCode.GAME_NOT_RUNNING, "The game has not started");
            case PAUSED -> throw reject(ErrorCode.GAME_PAUSED, "The game is paused");
            case FINISHED -> throw reject(ErrorCode.GAME_FINISHED, "The game is over");
            default -> throw reject(ErrorCode.UNKNOWN_ROOM, "Room is closed");
        }
    }
}
