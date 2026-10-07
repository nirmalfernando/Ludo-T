package com.ludot.events;

import com.ludot.TestBoard;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.flow.GameStartEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.events.flow.TurnStartEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OutputFormatter — all message formats")
class OutputFormatterTest {

    private Board board;

    private OutputFormatter formatter;

    @BeforeEach void setUp() {
        board = TestBoard.create();
        formatter = new OutputFormatter();
    }

    
    // ── helpers ───────────────────────────────────────────────────────────────

    private Player player(Colour c) {
        return new Player(c, List.of(), (gs, r, ps, m) -> m.get(0));
    }

    private String format(List<Player> order, Map<Colour, Integer> rolls) {
        return formatter.format(new TurnOrderEvent(order.get(0), order, rolls));
    }

    // ── turn order (PDF section 3.1: the highest roller starts, play passes clockwise) ──

    @Test @DisplayName("the highest roller is named as the starter")
    void namesTheHighestRoller() {
        List<Player> order = List.of(player(Colour.GREEN), player(Colour.YELLOW),
                                     player(Colour.BLUE),  player(Colour.RED));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 3, Colour.GREEN, 5, Colour.YELLOW, 1, Colour.BLUE, 4);
        String msg = format(order, rolls);
        assertTrue(msg.contains("Green player has the highest roll and will begin the game."), msg);
    }

    @Test @DisplayName("the order of a single round lists all four players, starting with the highest roller")
    void listsTheRoundOrder() {
        List<Player> order = List.of(player(Colour.BLUE), player(Colour.RED),
                                     player(Colour.GREEN), player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 1, Colour.GREEN, 2, Colour.YELLOW, 3, Colour.BLUE, 6);
        String msg = format(order, rolls);
        assertTrue(msg.contains("The order of a single round is Blue, Red, Green, and Yellow."), msg);
    }

    @Test @DisplayName("there is nothing about tie-breaking by name: ties are settled by rolling again")
    void noAlphabeticalWording() {
        List<Player> order = List.of(player(Colour.RED), player(Colour.GREEN),
                                     player(Colour.YELLOW), player(Colour.BLUE));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 6, Colour.GREEN, 6, Colour.YELLOW, 2, Colour.BLUE, 6);
        String msg = format(order, rolls);
        assertFalse(msg.toLowerCase().contains("alphabetical"), msg);
        assertFalse(msg.toLowerCase().contains("all players rolled"), msg);
    }

    // ── dice event formats ────────────────────────────────────────────────────

    @Test @DisplayName("DiceRollEvent format: '[colour] player rolled [value].'")
    void diceRollFormat() {
        String msg = formatter.format(new DiceRollEvent(player(Colour.RED), 4));
        assertEquals("Red player rolled 4.", msg);
    }

    @Test @DisplayName("TripleSixEvent format: turn forfeited when no block present")
    void tripleSixFormatNoBlock() {
        String msg = formatter.format(new TripleSixEvent(player(Colour.YELLOW), false));
        assertEquals("Yellow rolled three consecutive 6s! Turn forfeited.", msg);
    }

    @Test @DisplayName("TripleSixEvent format: forced block break message when block present")
    void tripleSixFormatWithBlock() {
        String msg = formatter.format(new TripleSixEvent(player(Colour.YELLOW), true));
        assertEquals("Yellow rolled three consecutive 6s! Forced block break! (T-6)", msg);
    }

    @Test @DisplayName("CoinTossEvent format: Heads for Clockwise, piece name included")
    void coinTossHeadsFormat() {
        Piece p = new Piece("R1", Colour.RED, board);
        String msg = formatter.format(new CoinTossEvent(p, Direction.CLOCKWISE));
        assertEquals("R1 exits base. Coin toss: Heads -> Clockwise", msg);
    }

    @Test @DisplayName("CoinTossEvent format: Tails for Counter-Clockwise")
    void coinTossTailsFormat() {
        Piece p = new Piece("G2", Colour.GREEN, board);
        String msg = formatter.format(new CoinTossEvent(p, Direction.COUNTER_CLOCKWISE));
        assertEquals("G2 exits base. Coin toss: Tails -> Counter-Clockwise", msg);
    }

    // ── move / capture formats ────────────────────────────────────────────────

    @Test @DisplayName("SkipTurnEvent format: '[colour] player does not have other pieces available. Ignoring the throw.'")
    void skipTurnFormat() {
        String msg = formatter.format(new SkipTurnEvent(player(Colour.BLUE)));
        assertEquals("Blue player does not have other pieces available. Ignoring the throw.", msg);
    }

    @Test @DisplayName("CaptureEvent format: two lines — spec capture message, bonus roll")
    void captureEventTwoLines() {
        Piece attacker = new Piece("R1", Colour.RED, board);
        Piece victim   = new Piece("B1", Colour.BLUE, board);
        Cell cell = board.getCell(9);
        String msg = formatter.format(new CaptureEvent(attacker, victim, cell, null));
        String[] lines = msg.split("\n");
        assertEquals(2, lines.length, "CaptureEvent must produce exactly 2 lines");
        assertEquals("Red piece R1 lands on square 9, captures Blue piece B1, and returns it to the base.", lines[0]);
        assertEquals("Red receives a bonus roll! (T-2)", lines[1]);
    }

    // ── block formats ─────────────────────────────────────────────────────────

    @Test @DisplayName("BlockFormEvent format: '[piece] forms a blockade with [piece] at cell [id] (T-3)'")
    void blockFormFormat() {
        Piece p1 = new Piece("R1", Colour.RED, board);
        Piece p2 = new Piece("R2", Colour.RED, board);
        Cell cell = board.getCell(9);
        String msg = formatter.format(new BlockFormEvent(List.of(p1, p2), cell));
        assertEquals("R1 forms a blockade with R2 at cell 9 (T-3)", msg);
    }

    // ── game flow formats ─────────────────────────────────────────────────────

    @Test @DisplayName("GameStartEvent format: starts with '=== LUDO-T GAME START ==='")
    void gameStartBanner() {
        String msg = formatter.format(new GameStartEvent(List.of(player(Colour.RED))));
        assertTrue(msg.startsWith("=== LUDO-T GAME START ==="),
                "GameStartEvent must start with the banner");
        assertTrue(msg.contains("Determining first player..."),
                "GameStartEvent must include roll-off prompt");
    }

    @Test @DisplayName("TurnStartEvent format: '--- [colour]'s turn ---'")
    void turnStartFormat() {
        String msg = formatter.format(new TurnStartEvent(player(Colour.GREEN)));
        assertTrue(msg.contains("--- Green's turn ---"));
    }

    @Test @DisplayName("WinEvent format: two lines — spec win message and round count")
    void winEventFormat() {
        String msg = formatter.format(new WinEvent(player(Colour.RED), 15));
        String[] lines = msg.split("\n");
        assertEquals(2, lines.length, "WinEvent must produce exactly 2 lines");
        assertEquals("Red player wins!!!", lines[0]);
        assertEquals("Game finished in 15 rounds.", lines[1]);
    }

    @Test @DisplayName("RoundEndEvent format: first line is '=== Round N Status ==='")
    void roundEndHeaderPresent() {
        String msg = formatter.format(RoundEndEvent.of(3, List.of(player(Colour.RED))));
        assertTrue(msg.startsWith("=== Round 3 Status ==="),
                "RoundEndEvent must start with the round-status header");
    }
}
