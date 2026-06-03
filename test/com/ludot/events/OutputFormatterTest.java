package com.ludot.events;

import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OutputFormatter - TurnOrderEvent messages")
class OutputFormatterTest {

    private OutputFormatter formatter;

    @BeforeEach void setUp() {
        formatter = new OutputFormatter();
    }

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
        assertTrue(msg.contains("Red player has the highest roll"),
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
        assertTrue(msg.contains("Red player has the highest roll"), "Winner should be named");
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
}
