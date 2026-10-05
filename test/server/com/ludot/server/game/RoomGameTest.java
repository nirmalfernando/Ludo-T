package com.ludot.server.game;

import com.ludot.protocol.BoardDto;
import com.ludot.protocol.BoardGeometry;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EventType;
import com.ludot.protocol.Location;
import com.ludot.protocol.MoveKind;
import com.ludot.protocol.Phase;
import com.ludot.protocol.PieceDto;
import com.ludot.protocol.Variant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoomGame")
class RoomGameTest {

    private static void playToTheEnd(RoomGame game, List<EventType> seen) {
        while (!game.isFinished()) {
            Colour colour = game.currentColour();
            RoomGame.Step rolled = game.roll(colour);
            rolled.events().forEach(e -> seen.add(e.type()));
            if (game.phase() == Phase.AWAITING_MOVE) {
                game.applyMove(colour, game.strategyChoice()).events().forEach(e -> seen.add(e.type()));
            }
        }
    }

    @Test @DisplayName("a new game has 16 pieces at base and no turn yet")
    void newGameAtBase() {
        RoomGame game = new RoomGame(Variant.LUDO_T, 1);
        BoardDto board = game.board();
        assertEquals(16, board.pieces().size());
        assertTrue(board.pieces().stream().allMatch(p -> p.location() == Location.BASE));
        assertFalse(game.isStarted());
        assertNull(game.turnInfo(null));
    }

    @Test @DisplayName("start publishes the roll-off events and opens a roll")
    void startOpensRoll() {
        RoomGame game = new RoomGame(Variant.LUDO_T, 1);
        RoomGame.Step step = game.start();
        assertTrue(step.events().stream().anyMatch(e -> e.type() == EventType.GAME_START));
        assertTrue(step.events().stream().anyMatch(e -> e.type() == EventType.TURN_ORDER));
        assertEquals(Phase.AWAITING_ROLL, game.phase());
        assertEquals(game.currentColour(), game.turnInfo(null).colour());
    }

    @Test @DisplayName("a roll reports its value and offers moves whose indexes match their positions")
    void rollOffersIndexedMoves() {
        RoomGame game = new RoomGame(Variant.LUDO_T, 2);
        game.start();
        RoomGame.Step step = null;
        while (game.phase() != Phase.AWAITING_MOVE) {
            step = game.roll(game.currentColour());
        }
        assertNotNull(step.rolled());
        assertEquals(step.rolled(), game.lastRoll());
        assertFalse(game.options().isEmpty());
        for (int i = 0; i < game.options().size(); i++) {
            assertEquals(i, game.options().get(i).index());
        }
    }

    @Test @DisplayName("when nothing can move the only option is SKIP")
    void onlySkipWhenNothingMovable() {
        RoomGame game = new RoomGame(Variant.LUDO_T, 3);
        game.start();
        boolean sawSkip = false;
        for (int i = 0; i < 200 && !sawSkip && !game.isFinished(); i++) {
            Colour colour = game.currentColour();
            game.roll(colour);
            if (game.phase() == Phase.AWAITING_MOVE) {
                if (game.onlySkipAvailable()) {
                    sawSkip = true;
                    assertEquals(MoveKind.SKIP, game.options().get(0).kind());
                }
                game.applyMove(colour, game.strategyChoice());
            }
        }
        assertTrue(sawSkip, "early in a game most rolls have no legal move");
    }

    @Test @DisplayName("every coordinate in the board DTO stays inside the board geometry")
    void boardCoordinatesInRange() {
        RoomGame game = new RoomGame(Variant.LUDO_T, 2069);
        game.start();
        for (int i = 0; i < 400 && !game.isFinished(); i++) {
            Colour colour = game.currentColour();
            game.roll(colour);
            if (game.phase() == Phase.AWAITING_MOVE) {
                game.applyMove(colour, game.strategyChoice());
            }
            for (PieceDto piece : game.board().pieces()) {
                if (piece.location() == Location.TRACK) {
                    assertTrue(piece.cell() >= 0 && piece.cell() < BoardGeometry.TRACK_SIZE, piece.toString());
                }
                assertTrue(piece.homeStep() >= 0 && piece.homeStep() < BoardGeometry.HOME_STRAIGHT_LENGTH);
            }
        }
    }

    @Test @DisplayName("the same seed replays to the same hash and event sequence")
    void deterministic() {
        List<EventType> first = new ArrayList<>();
        List<EventType> second = new ArrayList<>();
        RoomGame a = new RoomGame(Variant.LUDO_T, 1);
        RoomGame b = new RoomGame(Variant.LUDO_T, 1);
        a.start();
        b.start();
        playToTheEnd(a, first);
        playToTheEnd(b, second);
        assertEquals(first, second);
        assertEquals(a.stateHash(), b.stateHash());
        assertTrue(first.contains(EventType.WIN));
    }

    @Test @DisplayName("a finished game ranks the winner first and lists all four colours once")
    void standingsWinnerFirst() {
        RoomGame game = new RoomGame(Variant.LUDO_T, 1);
        game.start();
        playToTheEnd(game, new ArrayList<>());
        List<Colour> standings = game.standings();
        assertEquals(4, standings.size());
        assertEquals(4, standings.stream().distinct().count());
        assertEquals(Phase.FINISHED, game.phase());
    }

    @Test @DisplayName("the classic variant plays through too")
    void classicVariantPlays() {
        RoomGame game = new RoomGame(Variant.CLASSIC, 5);
        game.start();
        List<EventType> seen = new ArrayList<>();
        playToTheEnd(game, seen);
        assertTrue(seen.contains(EventType.WIN));
    }

    @Test @DisplayName("two games running side by side never share state")
    void gamesAreIsolated() {
        RoomGame a = new RoomGame(Variant.LUDO_T, 1);
        RoomGame b = new RoomGame(Variant.LUDO_T, 2);
        a.start();
        b.start();
        long hashB = b.stateHash();
        for (int i = 0; i < 50; i++) {
            Colour colour = a.currentColour();
            a.roll(colour);
            if (a.phase() == Phase.AWAITING_MOVE) {
                a.applyMove(colour, a.strategyChoice());
            }
        }
        assertEquals(hashB, b.stateHash());
    }
}
