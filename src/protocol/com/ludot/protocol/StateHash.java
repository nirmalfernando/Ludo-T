package com.ludot.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.zip.CRC32;

/**
 * Compact fingerprint of what a client should be showing. The server puts it in every update and
 * clients (and the stress client) recompute it from their own copy, so two screens that agree on the
 * hash agree on the game state.
 */
public final class StateHash {

    private StateHash() {
    }

    /** CRC32 over a canonical rendering of the board and the turn number. */
    public static long of(BoardDto board, int turnNumber) {
        StringBuilder canonical = new StringBuilder();
        canonical.append("turn=").append(turnNumber).append(";round=").append(board.round()).append(';');
        board.pieces().stream()
                .sorted(Comparator.comparing(PieceDto::colour).thenComparingInt(PieceDto::index))
                .forEach(p -> canonical
                        .append(p.colour()).append('|').append(p.index()).append('|').append(p.location()).append('|')
                        .append(p.cell()).append('|').append(p.homeStep()).append('|').append(p.direction()).append('|')
                        .append(p.effect()).append('|').append(p.effectRoundsLeft()).append('|')
                        .append(p.briefingRoundsLeft()).append('|').append(p.inBlock()).append(';'));
        board.mysteryCells().stream()
                .sorted(Comparator.comparingInt(MysteryCellDto::cell))
                .forEach(m -> canonical.append("mystery=").append(m.cell()).append('|').append(m.roundsLeft()).append(';'));
        for (Colour colour : Colour.values()) {
            canonical.append("captures.").append(colour).append('=')
                    .append(board.captureCounts().getOrDefault(colour, 0)).append(';');
        }
        CRC32 crc = new CRC32();
        crc.update(canonical.toString().getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
