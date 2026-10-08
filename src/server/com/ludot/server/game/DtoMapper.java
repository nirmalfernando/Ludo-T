package com.ludot.server.game;

import com.ludot.engine.GameEngine;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.state.AtHomeState;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.protocol.BoardDto;
import com.ludot.protocol.BoardGeometry;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Direction;
import com.ludot.protocol.EffectType;
import com.ludot.protocol.Location;
import com.ludot.protocol.MysteryCellDto;
import com.ludot.protocol.PieceDto;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class DtoMapper {

    private static final int NO_CELL = -1;

    private DtoMapper() {
    }

    static Colour colour(com.ludot.model.Colour colour) {
        return Colour.valueOf(colour.name());
    }

    static com.ludot.model.Colour coreColour(Colour colour) {
        return com.ludot.model.Colour.valueOf(colour.name());
    }

    // pieces are named after their colour's initial plus 1-4 ("R3"), so the index is the trailing digit minus one
    static int pieceIndex(Piece piece) {
        String name = piece.getName();
        return Character.getNumericValue(name.charAt(name.length() - 1)) - 1;
    }

    static Integer cellId(Cell cell) {
        return cell == null ? null : cell.getId();
    }

    static BoardDto board(GameEngine engine) {
        List<PieceDto> pieces = new ArrayList<>();
        Map<Colour, Integer> captures = new EnumMap<>(Colour.class);
        for (Player player : engine.getPlayers()) {
            int captured = 0;
            for (Piece piece : player.getPieces()) {
                pieces.add(piece(piece, engine.getBoard()));
                captured += piece.getCaptureCount();
            }
            captures.put(colour(player.getColour()), captured);
        }
        List<MysteryCellDto> mystery = engine.getMysteryCell() == null
                ? List.of()
                : List.of(new MysteryCellDto(engine.getMysteryCell().getId(), engine.getMysteryRoundsLeft()));
        return new BoardDto(pieces, mystery, captures, engine.getRoundCount());
    }

    static PieceDto piece(Piece piece, Board board) {
        Cell position = piece.getPosition();
        Location location = location(piece);
        int homeStep = homeStep(piece, board, location);
        int cell = location == Location.TRACK || location == Location.BRIEFING ? trackCell(position) : NO_CELL;
        int briefingLeft = piece.getState() instanceof AtBriefingState briefing ? briefing.getRoundsLeft() : 0;
        return new PieceDto(colour(piece.getColour()), pieceIndex(piece), location, cell, homeStep,
                direction(piece), effect(piece), piece.getActiveEffect().getRoundsLeft(), briefingLeft,
                position != null && position.hasBlock());
    }

    private static Location location(Piece piece) {
        if (piece.getState() instanceof AtBaseState) {
            return Location.BASE;
        }
        if (piece.getState() instanceof AtBriefingState) {
            return Location.BRIEFING;
        }
        if (piece.getState() instanceof InHomeStraightState) {
            return Location.HOME_STRAIGHT;
        }
        if (piece.getState() instanceof AtHomeState) {
            return Location.HOME;
        }
        return Location.TRACK;
    }

    private static int trackCell(Cell position) {
        return position == null ? NO_CELL : position.getId();
    }

    private static int homeStep(Piece piece, Board board, Location location) {
        if (location == Location.HOME) {
            return BoardGeometry.HOME_STRAIGHT_LENGTH - 1;
        }
        if (location != Location.HOME_STRAIGHT) {
            return 0;
        }
        return Math.max(0, board.getHomeStraight(piece.getColour()).indexOf(piece.getPosition()));
    }

    private static Direction direction(Piece piece) {
        return piece.getDirection() == null ? null : Direction.valueOf(piece.getDirection().name());
    }

    private static EffectType effect(Piece piece) {
        return switch (piece.getActiveEffect().getName()) {
            case "ENERGISED" -> EffectType.ENERGISED;
            case "SICK" -> EffectType.SICK;
            default -> EffectType.NONE;
        };
    }
}
