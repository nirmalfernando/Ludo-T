package com.ludot.client.view;

import com.ludot.protocol.Colour;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the "Who goes first?" panel shows, built from the roll-off events as they arrive: each colour's rolls, who
 * dropped out after a tie, who rolls next and, once the server announces it, the winner and the playing order.
 * The contenders roll one at a time in seat order, and after every full round only those who tied for the highest
 * roll stay in (spec 1.1). No JavaFX in here, so it can be tested.
 */
public final class RollOffTable {

    private static final List<Colour> SEATS = List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE);

    /** One colour's line: everything it rolled, whether it is out, and whether it won. */
    public record Row(Colour colour, List<Integer> rolls, boolean out, boolean winner) {
    }

    private final Map<Colour, List<Integer>> rolls = new EnumMap<>(Colour.class);
    private final Map<Colour, Integer> thisRound = new EnumMap<>(Colour.class);
    private List<Colour> contenders = new ArrayList<>(SEATS);
    private Colour winner;

    public RollOffTable() {
        reset();
    }

    /** Forgets everything: a new game is starting. */
    public void reset() {
        rolls.clear();
        SEATS.forEach(colour -> rolls.put(colour, new ArrayList<>()));
        thisRound.clear();
        contenders = new ArrayList<>(SEATS);
        winner = null;
    }

    /** Takes in a ROLL_OFF or TURN_ORDER event; anything else is ignored. */
    public void accept(EventDto event) {
        if (event.type() == EventType.ROLL_OFF && event.colour() != null && event.value() != null) {
            addRoll(event.colour(), event.value());
        } else if (event.type() == EventType.TURN_ORDER && event.colour() != null) {
            winner = event.colour();
        }
    }

    private void addRoll(Colour colour, int value) {
        rolls.get(colour).add(value);
        thisRound.put(colour, value);
        if (thisRound.size() == contenders.size()) {
            int highest = thisRound.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            contenders = new ArrayList<>(contenders.stream().filter(c -> thisRound.get(c) == highest).toList());
            thisRound.clear();
        }
    }

    public List<Row> rows() {
        return SEATS.stream().map(this::row).toList();
    }

    public Row row(Colour colour) {
        return new Row(colour, List.copyOf(rolls.get(colour)), !contenders.contains(colour), colour == winner);
    }

    /** Who has to roll next: the first contender that has not rolled in the current round; empty once one is left. */
    public Optional<Colour> nextRoller() {
        if (contenders.size() == 1) {
            return Optional.empty();
        }
        return contenders.stream().filter(colour -> !thisRound.containsKey(colour)).findFirst();
    }

    /** The only contender left after a round, before the server has announced the order. */
    public Optional<Colour> leader() {
        return contenders.size() == 1 ? Optional.of(contenders.get(0)) : Optional.empty();
    }

    /** True once anyone has rolled. */
    public boolean hasRolls() {
        return rolls.values().stream().anyMatch(list -> !list.isEmpty());
    }

    public boolean isDecided() {
        return winner != null;
    }

    public Optional<Colour> winner() {
        return Optional.ofNullable(winner);
    }

    /** Clockwise from the winner (the order of the colours R, G, Y, B); empty until it is announced. */
    public List<Colour> playOrder() {
        return winner == null ? List.of() : clockwiseFrom(winner);
    }

    /** The colours in playing order when {@code first} begins. */
    public static List<Colour> clockwiseFrom(Colour first) {
        return com.ludot.client.state.RoomModel.clockwiseFrom(first);
    }
}
