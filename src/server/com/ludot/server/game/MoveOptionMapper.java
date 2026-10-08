package com.ludot.server.game;

import com.ludot.GameConstants;
import com.ludot.command.BlockCaptureMove;
import com.ludot.command.BlockMove;
import com.ludot.command.CaptureMove;
import com.ludot.command.Move;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.command.SkipMove;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.protocol.Location;
import com.ludot.protocol.MoveKind;
import com.ludot.protocol.MoveOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

final class MoveOptionMapper {

    private static final int NONE = -1;

    private static final Map<Class<? extends Move>, BiFunction<Move, Integer, MoveOption>> MAPPERS = Map.of(
            MoveFromBase.class, (m, i) -> fromBase((MoveFromBase) m, i),
            MoveOnBoard.class, (m, i) -> onBoard((MoveOnBoard) m, i),
            CaptureMove.class, (m, i) -> capture((CaptureMove) m, i),
            BlockMove.class, (m, i) -> blockMove((BlockMove) m, i),
            BlockCaptureMove.class, (m, i) -> blockCapture((BlockCaptureMove) m, i),
            SkipMove.class, (m, i) -> skip(i));

    private MoveOptionMapper() {
    }

    static List<MoveOption> options(List<Move> moves) {
        List<MoveOption> options = new ArrayList<>(moves.size());
        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            BiFunction<Move, Integer, MoveOption> mapper = MAPPERS.get(move.getClass());
            if (mapper == null) {
                throw new IllegalStateException("No option mapping for " + move.getClass().getName());
            }
            options.add(mapper.apply(move, i));
        }
        return List.copyOf(options);
    }

    private static MoveOption fromBase(MoveFromBase move, int index) {
        Piece piece = move.getPiece();
        Cell start = piece.getBoard().getStartCell(piece.getColour());
        return new MoveOption(index, MoveKind.FROM_BASE, DtoMapper.pieceIndex(piece), NONE,
                Location.BASE, NONE, Location.TRACK, start.getId(), false, 0,
                start.isOccupied(), false, start.isMysteryCell(), false, false, move.describe());
    }

    private static MoveOption onBoard(MoveOnBoard move, int index) {
        Piece piece = move.getPiece();
        Board board = piece.getBoard();
        if (piece.getState() instanceof InHomeStraightState) {
            return homeStraightStep(move, index, board);
        }
        Cell source = piece.getPosition();
        Cell target = board.getCell(trackDestination(source.getId(), piece.getDirection(), move.getSteps()));
        boolean forms = target.isOccupied() && target.getPieces().get(0).getColour() == piece.getColour();
        return new MoveOption(index, MoveKind.MOVE, DtoMapper.pieceIndex(piece), NONE,
                Location.TRACK, source.getId(), Location.TRACK, target.getId(), false, 0,
                forms, source.hasBlock(), target.isMysteryCell(), false, false, move.describe());
    }

    private static MoveOption homeStraightStep(MoveOnBoard move, int index, Board board) {
        Piece piece = move.getPiece();
        List<Cell> straight = board.getHomeStraight(piece.getColour());
        int from = straight.indexOf(piece.getPosition());
        int to = from + move.getSteps();
        boolean reachesHome = to == straight.size() - 1;
        return new MoveOption(index, reachesHome ? MoveKind.REACH_HOME : MoveKind.MOVE,
                DtoMapper.pieceIndex(piece), NONE, Location.HOME_STRAIGHT, from,
                reachesHome ? Location.HOME : Location.HOME_STRAIGHT, to, false, 0,
                false, false, false, false, reachesHome, move.describe());
    }

    private static MoveOption capture(CaptureMove move, int index) {
        Piece attacker = move.getAttacker();
        Cell target = move.getTargetCell();
        return new MoveOption(index, MoveKind.CAPTURE, DtoMapper.pieceIndex(attacker), NONE,
                Location.TRACK, attacker.getPosition().getId(), Location.TRACK, target.getId(),
                true, 1, false, false, target.isMysteryCell(), false, false, move.describe());
    }

    private static MoveOption blockMove(BlockMove move, int index) {
        List<Piece> block = move.getBlock();
        Cell source = block.get(0).getPosition();
        return new MoveOption(index, MoveKind.BLOCK_MOVE, DtoMapper.pieceIndex(block.get(0)),
                DtoMapper.pieceIndex(block.get(1)), Location.TRACK, source.getId(), Location.TRACK,
                move.getTargetCell().getId(), false, 0, false, false,
                move.getTargetCell().isMysteryCell(), false, false, move.describe());
    }

    private static MoveOption blockCapture(BlockCaptureMove move, int index) {
        List<Piece> attackers = move.getAttackers();
        Cell source = attackers.get(0).getPosition();
        return new MoveOption(index, MoveKind.BLOCK_CAPTURE, DtoMapper.pieceIndex(attackers.get(0)),
                DtoMapper.pieceIndex(attackers.get(1)), Location.TRACK, source.getId(), Location.TRACK,
                move.getTargetCell().getId(), true, move.getVictims().size(), false, false,
                move.getTargetCell().isMysteryCell(), false, false, move.describe());
    }

    private static MoveOption skip(int index) {
        return new MoveOption(index, MoveKind.SKIP, NONE, NONE, Location.BASE, NONE, Location.BASE, NONE,
                false, 0, false, false, false, false, false, "No valid moves: the turn is skipped");
    }

    private static int trackDestination(int from, Direction direction, int steps) {
        int size = GameConstants.BOARD_SIZE;
        int delta = direction == Direction.CLOCKWISE ? steps : -steps;
        return ((from + delta) % size + size) % size;
    }
}
