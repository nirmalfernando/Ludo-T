package com.ludot.bot;

import com.ludot.protocol.ErrorCode;
import java.util.EnumSet;
import java.util.Set;

/**
 * The kinds of request the stress client sends besides legitimate moves, with how often each is
 * picked and which outcomes count as correct. Anything else the server answers is UNEXPECTED.
 */
enum RequestKind {

    STALE_MOVE(15, false, ErrorCode.STALE_TURN, ErrorCode.NOT_YOUR_TURN, ErrorCode.NOT_A_PLAYER),
    OUT_OF_TURN_ROLL(10, true, ErrorCode.NOT_YOUR_TURN, ErrorCode.NOT_A_PLAYER, ErrorCode.STALE_TURN,
            ErrorCode.WRONG_PHASE),
    DUPLICATE(10, true),
    SNAPSHOT(10, true),
    LIST_ROOMS(5, true),
    CHAT(5, true, ErrorCode.CHAT_RATE_LIMITED),
    CLAIM_TAKEN(5, false, ErrorCode.SEAT_TAKEN, ErrorCode.SEATS_LOCKED),
    PAUSE_NOT_HOST(5, false, ErrorCode.NOT_HOST),
    BAD_INDEX(5, false, ErrorCode.INVALID_MOVE, ErrorCode.WRONG_PHASE, ErrorCode.NOT_YOUR_TURN,
            ErrorCode.STALE_TURN, ErrorCode.NOT_A_PLAYER),
    /** A real roll or move for our own turn: it races with other sender threads, so a late one is stale. */
    LEGIT(0, true, ErrorCode.STALE_TURN, ErrorCode.WRONG_PHASE, ErrorCode.NOT_YOUR_TURN);

    private final int weight;
    private final boolean okAllowed;
    private final Set<ErrorCode> allowed;

    RequestKind(int weight, boolean okAllowed, ErrorCode... allowed) {
        this.weight = weight;
        this.okAllowed = okAllowed;
        this.allowed = allowed.length == 0 ? EnumSet.noneOf(ErrorCode.class) : EnumSet.of(allowed[0], allowed);
        // the game being over or paused is always an acceptable reason for a gameplay request to be refused
        this.allowed.add(ErrorCode.GAME_FINISHED);
        this.allowed.add(ErrorCode.GAME_PAUSED);
        this.allowed.add(ErrorCode.GAME_NOT_RUNNING);
    }

    int weight() {
        return weight;
    }

    boolean accepts(boolean ok, ErrorCode code) {
        return ok ? okAllowed : allowed.contains(code);
    }

    /** Picks a kind by weight from a number in [0, 1); LEGIT has weight 0 because it is sent whenever our turn comes. */
    static RequestKind pick(double unit) {
        int total = 0;
        for (RequestKind kind : values()) {
            total += kind.weight;
        }
        double target = unit * total;
        int cumulative = 0;
        for (RequestKind kind : values()) {
            cumulative += kind.weight;
            if (kind.weight > 0 && target < cumulative) {
                return kind;
            }
        }
        return SNAPSHOT;
    }
}
