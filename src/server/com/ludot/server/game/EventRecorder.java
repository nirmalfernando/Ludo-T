package com.ludot.server.game;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.ArrayList;
import java.util.List;

/** Builds {@link EventDto}s from core objects and keeps them until they are collected. */
final class EventRecorder {

    private final List<EventDto> collected = new ArrayList<>();

    /** Records one event; colour, piece and the cells may be null when they do not apply, and text may be null. */
    void add(EventType type, Colour colour, Piece piece, Cell from, Cell to, String text) {
        add(type, colour, piece, from, to, text, null);
    }

    /** As above, for an event that carries a number (see {@link EventDto}). */
    void add(EventType type, Colour colour, Piece piece, Cell from, Cell to, String text, Integer value) {
        collected.add(new EventDto(type, colour, piece == null ? null : DtoMapper.pieceIndex(piece),
                DtoMapper.cellId(from), DtoMapper.cellId(to), text == null ? "" : text, value));
    }

    /** Returns what was recorded since the previous call and clears the buffer. */
    List<EventDto> drain() {
        List<EventDto> drained = List.copyOf(collected);
        collected.clear();
        return drained;
    }

    static Colour colourOf(Player player) {
        return DtoMapper.colour(player.getColour());
    }

    static Colour colourOf(Piece piece) {
        return DtoMapper.colour(piece.getColour());
    }
}
