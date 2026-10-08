package com.ludot.protocol;

import java.util.List;
import java.util.Map;

// sent whole with every update so clients never compute state
public record BoardDto(List<PieceDto> pieces, List<MysteryCellDto> mysteryCells, Map<Colour, Integer> captureCounts, int round) implements java.io.Serializable {

    public BoardDto {
        pieces = List.copyOf(pieces);
        mysteryCells = List.copyOf(mysteryCells);
        captureCounts = Map.copyOf(captureCounts);
    }
}
