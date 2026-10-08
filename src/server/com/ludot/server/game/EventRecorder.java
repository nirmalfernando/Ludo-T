package com.ludot.server.game;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.ArrayList;
import java.util.List;

final class EventRecorder {

    private final List<EventDto> collected = new ArrayList<>();

    void add(EventType type, Colour colour, Piece piece, Cell from, Cell to, String text) {
        add(type, colour, piece, from, to, text, null);
    }

    void add(EventType type, Colour colour, Piece piece, Cell from, Cell to, String text, Integer value) {
        collected.add(new EventDto(type, colour, piece == null ? null : DtoMapper.pieceIndex(piece),
                DtoMapper.cellId(from), DtoMapper.cellId(to), text == null ? "" : text, value));
    }

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
