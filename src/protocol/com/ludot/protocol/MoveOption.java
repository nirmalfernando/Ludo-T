package com.ludot.protocol;

import java.util.Objects;

// one legal move offered for the current roll. partnerPieceIndex is -1 unless it is a block move
public record MoveOption(int index, MoveKind kind, int pieceIndex, int partnerPieceIndex, Location from, int fromCell, Location to, int toCell, boolean captures, int capturedCount, boolean formsBlock, boolean breaksBlock, boolean landsOnMystery, boolean entersHomeStraight, boolean reachesHome, String description) implements java.io.Serializable {

    public MoveOption {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(description, "description");
    }
}
