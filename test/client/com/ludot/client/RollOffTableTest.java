package com.ludot.client;

import com.ludot.client.view.RollOffTable;
import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Roll-off table: what the 'Who goes first?' panel shows")
class RollOffTableTest {

    private static EventDto roll(Colour colour, int value) {
        return new EventDto(EventType.ROLL_OFF, colour, null, null, null, colour + " rolls " + value, value);
    }

    private static EventDto order(Colour first) {
        return new EventDto(EventType.TURN_ORDER, first, null, null, null, first + " begins", null);
    }

    private static RollOffTable rolled(int red, int green, int yellow, int blue) {
        RollOffTable table = new RollOffTable();
        table.accept(roll(Colour.RED, red));
        table.accept(roll(Colour.GREEN, green));
        table.accept(roll(Colour.YELLOW, yellow));
        table.accept(roll(Colour.BLUE, blue));
        return table;
    }

    @Test @DisplayName("there is always one row per colour, in seat order, even before anyone has rolled")
    void fourRowsFromTheStart() {
        RollOffTable table = new RollOffTable();
        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE),
                table.rows().stream().map(RollOffTable.Row::colour).toList());
        assertTrue(table.rows().stream().allMatch(row -> row.rolls().isEmpty()));
        assertEquals(Optional.of(Colour.RED), table.nextRoller(), "Red rolls first");
        assertFalse(table.isDecided());
    }

    @Test @DisplayName("the next roller moves along the seats as each roll arrives")
    void nextRollerFollowsTheRolls() {
        RollOffTable table = new RollOffTable();
        table.accept(roll(Colour.RED, 3));
        assertEquals(Optional.of(Colour.GREEN), table.nextRoller());
        table.accept(roll(Colour.GREEN, 5));
        assertEquals(Optional.of(Colour.YELLOW), table.nextRoller());
    }

    @Test @DisplayName("a clear winner is the highest roller; the order announced by the server is shown clockwise from them")
    void aClearWinner() {
        RollOffTable table = rolled(3, 5, 1, 4);
        assertEquals(Optional.of(Colour.GREEN), table.leader());
        assertFalse(table.isDecided(), "decided only when the server announces the order");
        table.accept(order(Colour.GREEN));
        assertTrue(table.isDecided());
        assertEquals(Optional.of(Colour.GREEN), table.winner());
        assertEquals(List.of(Colour.GREEN, Colour.YELLOW, Colour.BLUE, Colour.RED), table.playOrder());
        assertTrue(table.rows().stream().filter(RollOffTable.Row::winner).map(RollOffTable.Row::colour)
                .toList().equals(List.of(Colour.GREEN)));
    }

    @Test @DisplayName("a tie for the highest roll: the others drop out and only the tied players roll again")
    void aTieRollsAgain() {
        RollOffTable table = rolled(4, 4, 2, 4);
        assertTrue(table.row(Colour.YELLOW).out());
        assertFalse(table.row(Colour.RED).out());
        assertEquals(Optional.empty(), table.leader(), "three players still tied");
        assertEquals(Optional.of(Colour.RED), table.nextRoller());

        table.accept(roll(Colour.RED, 3));
        assertEquals(Optional.of(Colour.GREEN), table.nextRoller());
        table.accept(roll(Colour.GREEN, 6));
        assertEquals(Optional.of(Colour.BLUE), table.nextRoller(), "Yellow is skipped");
        table.accept(roll(Colour.BLUE, 5));

        assertEquals(List.of(4, 3), table.row(Colour.RED).rolls());
        assertEquals(List.of(4, 6), table.row(Colour.GREEN).rolls());
        assertEquals(List.of(2), table.row(Colour.YELLOW).rolls());
        assertEquals(List.of(4, 5), table.row(Colour.BLUE).rolls());
        assertEquals(Optional.of(Colour.GREEN), table.leader());
        assertEquals(Optional.empty(), table.nextRoller(), "nobody left to roll");
    }

    @Test @DisplayName("a new game starts from an empty table")
    void resetForANewGame() {
        RollOffTable table = rolled(3, 5, 1, 4);
        table.accept(order(Colour.GREEN));
        table.reset();
        assertFalse(table.isDecided());
        assertTrue(table.rows().stream().allMatch(row -> row.rolls().isEmpty() && !row.out()));
        assertEquals(Optional.of(Colour.RED), table.nextRoller());
    }
}
