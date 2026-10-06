package com.ludot.server.app;

import com.ludot.protocol.Role;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.Speed;
import com.ludot.protocol.Variant;
import com.ludot.server.game.RoomGame;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Everything the parts of a room share. It is plain data with no behaviour of its own: the room's
 * components (membership, seating, game commands, turn timing, chat, broadcasting) each own one job and
 * read or change this state. All of it is confined to the room's single thread, so it needs no locks;
 * only {@code summary}, which other threads read, is volatile.
 */
final class RoomState {

    final String roomId;
    final RoomMode mode;
    final Variant variant;
    final long seed;
    final SessionConfig config;
    final LongSupplier clock;
    final RoomGame game;
    final SeatTable seats = new SeatTable();
    final ChatChannel chat;
    final Map<String, Member> members = new LinkedHashMap<>();
    final Deque<String> log = new ArrayDeque<>();

    RoomStatus status = RoomStatus.WAITING;
    Speed speed = Speed.NORMAL;
    String hostId;
    boolean autoPaused;
    Long deadline;                  // epoch ms by which a person must act, or null

    volatile RoomSummary summary;

    RoomState(String roomId, RoomMode mode, Variant variant, long seed, SessionConfig config, LongSupplier clock) {
        this.roomId = roomId;
        this.mode = mode;
        this.variant = variant;
        this.seed = seed;
        this.config = config;
        this.clock = clock;
        this.game = new RoomGame(variant, seed);
        this.chat = new ChatChannel(config, clock);
    }

    Role roleOf(String playerId) {
        return seats.seatOf(playerId).isPresent() ? Role.PLAYER : Role.SPECTATOR;
    }

    /** The host, or null when the room is empty. */
    Member host() {
        return hostId == null ? null : members.get(hostId);
    }
}
