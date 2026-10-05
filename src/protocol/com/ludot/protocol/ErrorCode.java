package com.ludot.protocol;

/** Reasons a request is rejected. A duplicate request id is not an error: the original response is replayed. */
public enum ErrorCode {
    /** Fails the format rule. */
    INVALID_NICKNAME,
    /** Another connected user has it. */
    NICKNAME_IN_USE,
    /** Reconnect token expired or unknown. */
    UNKNOWN_TOKEN,
    /** No such room, or it is closed. */
    UNKNOWN_ROOM,
    /** The sender isn't a member. */
    NOT_IN_ROOM,
    /** A host-only action from a non-host. */
    NOT_HOST,
    /** A spectator tried a gameplay action. */
    NOT_A_PLAYER,
    /** The seat was claimed first by someone else. */
    SEAT_TAKEN,
    /** Seat change in a Bot match or after the start. */
    SEATS_LOCKED,
    /** StartGame with no human seats. */
    NO_HUMAN_SEATS,
    /** Gameplay while the room is WAITING. */
    GAME_NOT_RUNNING,
    /** Gameplay while paused. */
    GAME_PAUSED,
    /** Gameplay after the end. */
    GAME_FINISHED,
    /** The active seat isn't controlled by the sender. */
    NOT_YOUR_TURN,
    /** turnNumber doesn't match the current turn. */
    STALE_TURN,
    /** Roll during AWAITING_MOVE or ChooseMove during AWAITING_ROLL. */
    WRONG_PHASE,
    /** Option index out of range. */
    INVALID_MOVE,
    /** The room's command queue is full (backpressure). */
    SERVER_BUSY,
    /** More than 5 chat messages in 5 s from this player in this channel. */
    CHAT_RATE_LIMITED,
    /** Chat text empty after trimming, or longer than 200 characters. */
    CHAT_INVALID,
    /** Malformed or oversized message, or one sent before Hello. */
    PROTOCOL_ERROR,
    /** Unexpected server exception (logged with the request id). */
    INTERNAL_ERROR
}
