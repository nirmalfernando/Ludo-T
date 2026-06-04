package com.ludot.events;

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

    private OutputFormatter formatter;

    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        formatter = new OutputFormatter();
    }

    @AfterEach void tearDown() { Board.resetInstance(); }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Player player(Colour c) {
        return new Player(c, List.of(), (gs, r, ps, m) -> m.get(0));
    }

    private String format(List<Player> order, Map<Colour, Integer> rolls) {
        return formatter.format(new TurnOrderEvent(order.get(0), order, rolls));
    }

    // ── scenario 1: all equal rolls ───────────────────────────────────────────

    @Test @DisplayName("all equal: message says all players rolled the same value")
    void allEqualMentionsSharedValue() {
        List<Player> order = List.of(player(Colour.BLUE), player(Colour.GREEN),
                                     player(Colour.RED),  player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 3, Colour.GREEN, 3, Colour.YELLOW, 3, Colour.BLUE, 3);
        String msg = format(order, rolls);
        assertTrue(msg.contains("All players rolled 3"),
                "Should mention all players rolled 3");
    }

    @Test @DisplayName("all equal: message says order is determined alphabetically")
    void allEqualMentionsAlphabetical() {
        List<Player> order = List.of(player(Colour.BLUE), player(Colour.GREEN),
                                     player(Colour.RED),  player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 3, Colour.GREEN, 3, Colour.YELLOW, 3, Colour.BLUE, 3);
        String msg = format(order, rolls);
        assertTrue(msg.toLowerCase().contains("alphabetically"),
                "Should mention alphabetical ordering");
    }

    @Test @DisplayName("all equal: message does NOT say any player has the highest roll")
    void allEqualNoHighestRollClaim() {
        List<Player> order = List.of(player(Colour.BLUE), player(Colour.GREEN),
                                     player(Colour.RED),  player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 3, Colour.GREEN, 3, Colour.YELLOW, 3, Colour.BLUE, 3);
        String msg = format(order, rolls);
        assertFalse(msg.contains("highest roll"),
                "Should not claim any player has the highest roll when all tied");
    }

    @Test @DisplayName("all equal: play order line lists all four players")
    void allEqualOrderLinePresent() {
        List<Player> order = List.of(player(Colour.BLUE), player(Colour.GREEN),
                                     player(Colour.RED),  player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 3, Colour.GREEN, 3, Colour.YELLOW, 3, Colour.BLUE, 3);
        String msg = format(order, rolls);
        assertTrue(msg.contains("The order of play is Blue, Green, Red, and Yellow."),
                "Should list the alphabetical play order");
    }

    // ── scenario 2: no ties ───────────────────────────────────────────────────

    @Test @DisplayName("no ties: message names the highest roller")
    void noTiesNamesHighestRoller() {
        // RED=5, BLUE=4, GREEN=2, YELLOW=1 → order: RED, BLUE, GREEN, YELLOW
        List<Player> order = List.of(player(Colour.RED),   player(Colour.BLUE),
                                     player(Colour.GREEN), player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 5, Colour.GREEN, 2, Colour.YELLOW, 1, Colour.BLUE, 4);
        String msg = format(order, rolls);
        assertTrue(msg.contains("Red goes first!"),
                "Should name the highest roller");
    }

    @Test @DisplayName("no ties: message does NOT mention alphabetical ordering")
    void noTiesNoAlphabeticalMention() {
        List<Player> order = List.of(player(Colour.RED),   player(Colour.BLUE),
                                     player(Colour.GREEN), player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 5, Colour.GREEN, 2, Colour.YELLOW, 1, Colour.BLUE, 4);
        String msg = format(order, rolls);
        assertFalse(msg.toLowerCase().contains("alphabetically"),
                "Should not mention alphabetical when there are no ties");
    }

    @Test @DisplayName("no ties: play order line is present and correct")
    void noTiesOrderLineCorrect() {
        List<Player> order = List.of(player(Colour.RED),   player(Colour.BLUE),
                                     player(Colour.GREEN), player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 5, Colour.GREEN, 2, Colour.YELLOW, 1, Colour.BLUE, 4);
        String msg = format(order, rolls);
        assertTrue(msg.contains("The order of play is Red, Blue, Green, and Yellow."),
                "Play order line should reflect descending roll order");
    }

    // ── scenario 3: two-way tie ───────────────────────────────────────────────

    @Test @DisplayName("two-way tie: message names the tied players and their shared roll")
    void twoWayTieNamesPlayers() {
        // RED=5, GREEN=5 tied at top; YELLOW=2, BLUE=1 → order: GREEN, RED, YELLOW, BLUE
        List<Player> order = List.of(player(Colour.GREEN), player(Colour.RED),
                                     player(Colour.YELLOW), player(Colour.BLUE));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 5, Colour.GREEN, 5, Colour.YELLOW, 2, Colour.BLUE, 1);
        String msg = format(order, rolls);
        assertTrue(msg.contains("Green and Red") && msg.contains("rolled 5"),
                "Should name both tied players and the shared roll value");
    }

    @Test @DisplayName("two-way tie: message says tied players are ordered alphabetically")
    void twoWayTieMentionsAlphabetical() {
        List<Player> order = List.of(player(Colour.GREEN), player(Colour.RED),
                                     player(Colour.YELLOW), player(Colour.BLUE));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 5, Colour.GREEN, 5, Colour.YELLOW, 2, Colour.BLUE, 1);
        String msg = format(order, rolls);
        assertTrue(msg.toLowerCase().contains("alphabetically"),
                "Should mention alphabetical ordering for the tied group");
    }

    @Test @DisplayName("two-way tie: non-tied players are not mentioned in alphabetical note")
    void twoWayTieOnlyTiedPlayersInNote() {
        List<Player> order = List.of(player(Colour.GREEN), player(Colour.RED),
                                     player(Colour.YELLOW), player(Colour.BLUE));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 5, Colour.GREEN, 5, Colour.YELLOW, 2, Colour.BLUE, 1);
        String msg = format(order, rolls);
        // "Yellow" and "Blue" should only appear in the order line, not in the tie note
        String tieNoteLine = msg.lines()
                .filter(l -> l.toLowerCase().contains("alphabetically"))
                .findFirst().orElse("");
        assertFalse(tieNoteLine.contains("Yellow") || tieNoteLine.contains("Blue"),
                "Non-tied players should not appear in the alphabetical tie note");
    }

    // ── scenario 4: two-way tie at bottom ────────────────────────────────────

    @Test @DisplayName("two-way tie at bottom: winner named, tied group noted separately")
    void twoWayTieAtBottomWinnerAndTieNote() {
        // RED=6 clear winner; GREEN=2, YELLOW=2 tied at bottom
        List<Player> order = List.of(player(Colour.RED),   player(Colour.GREEN),
                                     player(Colour.YELLOW), player(Colour.BLUE));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 6, Colour.GREEN, 2, Colour.YELLOW, 2, Colour.BLUE, 1);
        String msg = format(order, rolls);
        assertTrue(msg.contains("Red goes first!"), "Winner should be named");
        assertTrue(msg.contains("Green and Yellow") && msg.contains("rolled 2"),
                "Tied pair should be noted");
    }

    // ── scenario 5: three-way tie ─────────────────────────────────────────────

    @Test @DisplayName("three-way tie: all three tied players named in the note")
    void threeWayTieAllPlayersNamed() {
        // RED=4, GREEN=4, BLUE=4 tied; YELLOW=1 last → order: BLUE, GREEN, RED, YELLOW
        List<Player> order = List.of(player(Colour.BLUE),   player(Colour.GREEN),
                                     player(Colour.RED),    player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 4, Colour.GREEN, 4, Colour.YELLOW, 1, Colour.BLUE, 4);
        String msg = format(order, rolls);
        assertTrue(msg.contains("Blue") && msg.contains("Green") && msg.contains("Red"),
                "All three tied players should appear in the tie note");
        assertTrue(msg.contains("rolled 4"), "Shared roll value should be mentioned");
    }

    @Test @DisplayName("three-way tie: play order is alphabetical for the tied group")
    void threeWayTieOrderAlphabetical() {
        List<Player> order = List.of(player(Colour.BLUE),   player(Colour.GREEN),
                                     player(Colour.RED),    player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 4, Colour.GREEN, 4, Colour.YELLOW, 1, Colour.BLUE, 4);
        String msg = format(order, rolls);
        assertTrue(msg.contains("The order of play is Blue, Green, Red, and Yellow."),
                "Tied group should be sorted alphabetically in the order line");
    }

    // ── scenario 6: two separate tie groups ──────────────────────────────────

    @Test @DisplayName("two separate tie groups: each group gets its own alphabetical note")
    void twoTieGroupsBothNoted() {
        // RED=4, GREEN=4 tied high; YELLOW=2, BLUE=2 tied low
        // order: GREEN, RED, BLUE, YELLOW
        List<Player> order = List.of(player(Colour.GREEN), player(Colour.RED),
                                     player(Colour.BLUE),  player(Colour.YELLOW));
        Map<Colour, Integer> rolls = Map.of(
                Colour.RED, 4, Colour.GREEN, 4, Colour.YELLOW, 2, Colour.BLUE, 2);
        String msg = format(order, rolls);
        assertTrue(msg.contains("rolled 4"), "High tie group should be noted");
        assertTrue(msg.contains("rolled 2"), "Low tie group should be noted");
        long alphaCount = msg.lines()
                .filter(l -> l.toLowerCase().contains("alphabetically"))
                .count();
        assertEquals(2, alphaCount,
                "Should have two separate alphabetical notes, one per tie group");
    }

    // ── dice event formats ────────────────────────────────────────────────────

    @Test @DisplayName("DiceRollEvent format: '[colour] rolls [value]'")
    void diceRollFormat() {
        String msg = formatter.format(new DiceRollEvent(player(Colour.RED), 4));
        assertEquals("Red rolls 4", msg);
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
        Piece p = new Piece("R1", Colour.RED);
        String msg = formatter.format(new CoinTossEvent(p, Direction.CLOCKWISE));
        assertEquals("R1 exits base. Coin toss: Heads -> Clockwise", msg);
    }

    @Test @DisplayName("CoinTossEvent format: Tails for Counter-Clockwise")
    void coinTossTailsFormat() {
        Piece p = new Piece("G2", Colour.GREEN);
        String msg = formatter.format(new CoinTossEvent(p, Direction.COUNTER_CLOCKWISE));
        assertEquals("G2 exits base. Coin toss: Tails -> Counter-Clockwise", msg);
    }

    // ── move / capture formats ────────────────────────────────────────────────

    @Test @DisplayName("SkipTurnEvent format: '[colour] has no valid moves. Turn skipped.'")
    void skipTurnFormat() {
        String msg = formatter.format(new SkipTurnEvent(player(Colour.BLUE)));
        assertEquals("Blue has no valid moves. Turn skipped.", msg);
    }

    @Test @DisplayName("CaptureEvent format: three lines — capture, sent to base, bonus roll")
    void captureEventThreeLines() {
        Piece attacker = new Piece("R1", Colour.RED);
        Piece victim   = new Piece("B1", Colour.BLUE);
        Cell cell = Board.getInstance().getCell(9);
        String msg = formatter.format(new CaptureEvent(attacker, victim, cell, null));
        String[] lines = msg.split("\n");
        assertEquals(3, lines.length, "CaptureEvent must produce exactly 3 lines");
        assertEquals("R1 captures B1 at cell 9!", lines[0]);
        assertEquals("B1 is sent to base.", lines[1]);
        assertEquals("Red receives a bonus roll! (T-2)", lines[2]);
    }

    // ── block formats ─────────────────────────────────────────────────────────

    @Test @DisplayName("BlockFormEvent format: '[piece] forms a blockade with [piece] at cell [id] (T-3)'")
    void blockFormFormat() {
        Piece p1 = new Piece("R1", Colour.RED);
        Piece p2 = new Piece("R2", Colour.RED);
        Cell cell = Board.getInstance().getCell(9);
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

    @Test @DisplayName("WinEvent format: two lines — banner and round count")
    void winEventFormat() {
        String msg = formatter.format(new WinEvent(player(Colour.RED), 15));
        String[] lines = msg.split("\n");
        assertEquals(2, lines.length, "WinEvent must produce exactly 2 lines");
        assertEquals("=== RED WINS! ===", lines[0]);
        assertEquals("Game finished in 15 rounds.", lines[1]);
    }

    @Test @DisplayName("RoundEndEvent format: first line is '=== Round N Status ==='")
    void roundEndHeaderPresent() {
        String msg = formatter.format(new RoundEndEvent(3, List.of(player(Colour.RED))));
        assertTrue(msg.startsWith("=== Round 3 Status ==="),
                "RoundEndEvent must start with the round-status header");
    }
}
