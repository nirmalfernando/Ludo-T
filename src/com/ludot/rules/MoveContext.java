package com.ludot.rules;

import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;

public record MoveContext(Piece piece, Player player, int roll, Cell destination) {
}
