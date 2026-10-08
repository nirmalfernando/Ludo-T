package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Opening roll-off in a room")
class RollOffSessionTest {

    private static final long NEVER_MS = 3_600_000;

    private Kit kit;

    @BeforeEach void setUp() {
        kit = new Kit();
    }

    @AfterEach void tearDown() {
        kit.close();
    }

    private static UUID id() {
        return UUID.randomUUID();
    }

    private static void assertRejected(ErrorCode expected, Response response) {
        assertEquals(expected, response.errorCode(), String.valueOf(response.message()));
        assertFalse(response.isOk());
    }

    private static void startWithAliceAsRed(GameSession room, Participant alice) {
        Kit.call(room, alice, new ClientMessage.ClaimSeat(id(), room.roomId(), Colour.RED));
        Kit.call(room, alice, new ClientMessage.StartGame(id(), room.roomId()));
    }

    private static List<EventDto> eventsOfType(Participant who, EventType type) {
        return Kit.sink(who).of(ServerMessage.StateUpdate.class).stream()
                .flatMap(update -> update.events().stream()).filter(e -> e.type() == type).toList();
    }

    private static void awaitOrderAnnounced(Participant who) {
        Kit.await(() -> !eventsOfType(who, EventType.TURN_ORDER).isEmpty(), 20_000);
    }

    private ServerMessage.YourTurn firstOffer(Participant alice) {
        return Kit.sink(alice).await(ServerMessage.YourTurn.class, 10_000);
    }

    @Test @DisplayName("a person is asked to roll for the order; the AI seats then roll one after another, in seat order")
    void aPersonRollsForTheOrderAndTheAiFollows() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        startWithAliceAsRed(room, alice);

        ServerMessage.YourTurn offer = firstOffer(alice);
        assertEquals(Phase.ROLL_OFF, offer.phase());
        assertEquals(0, offer.turnNumber());
        assertNull(offer.roll());
        assertTrue(offer.options().isEmpty());

        Response rolled = Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), offer.turnNumber()));
        assertTrue(rolled.isOk());
        int value = (Integer) rolled.payload();
        assertTrue(value >= 1 && value <= 6);

        awaitOrderAnnounced(alice);
        List<EventDto> rolls = eventsOfType(alice, EventType.ROLL_OFF);
        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE),
                rolls.stream().limit(4).map(EventDto::colour).toList());
        assertEquals(value, rolls.get(0).value(), "the number the person was told is the one in the event");
    }

    @Test @DisplayName("each roll-off roll is recorded as a step like any other, numbered from 0, so the stored game is complete")
    void rollOffRollsAreRecordedSteps() {
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        startWithAliceAsRed(room, alice);
        ServerMessage.YourTurn offer = firstOffer(alice);
        Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), offer.turnNumber()));
        awaitOrderAnnounced(alice);

        int rollOffRolls = eventsOfType(alice, EventType.ROLL_OFF).size();
        Kit.await(() -> kit.repository.decisions().size() >= rollOffRolls, 10_000);
        var steps = kit.repository.decisions().stream().sorted(java.util.Comparator.comparingInt(d -> d.turnNumber()))
                .limit(rollOffRolls).toList();
        for (int i = 0; i < rollOffRolls; i++) {
            assertEquals(i, steps.get(i).turnNumber());
            assertNull(steps.get(i).optionIndex(), "a roll, not a move");
        }
        assertEquals(DecisionSource.HUMAN, steps.get(0).source(), "alice pressed Roll");
        assertEquals(DecisionSource.AI, steps.get(1).source());
    }

    @Test @DisplayName("only the contender whose turn it is may roll; stale, out-of-phase and repeated requests are refused or answered once")
    void validationDuringTheRollOff() {
        kit.config = com.ludot.server.app.SessionConfig.defaults().withDelays(0, 1, 1).withTurnTimeoutMs(NEVER_MS);
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        Kit.call(room, bob, Kit.join(room));
        startWithAliceAsRed(room, alice);
        ServerMessage.YourTurn offer = firstOffer(alice);

        assertRejected(ErrorCode.NOT_A_PLAYER, Kit.call(room, bob, new ClientMessage.Roll(id(), room.roomId(), 0)));
        assertRejected(ErrorCode.STALE_TURN, Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), 7)));
        assertRejected(ErrorCode.WRONG_PHASE,
                Kit.call(room, alice, new ClientMessage.ChooseMove(id(), room.roomId(), offer.turnNumber(), 0)));

        UUID same = id();
        Response first = Kit.call(room, alice, new ClientMessage.Roll(same, room.roomId(), offer.turnNumber()));
        Response again = Kit.call(room, alice, new ClientMessage.Roll(same, room.roomId(), offer.turnNumber()));
        assertTrue(first.isOk());
        assertEquals(first.payload(), again.payload(), "a repeated request id gets the original answer");
        awaitOrderAnnounced(alice);
        assertEquals(1, eventsOfType(alice, EventType.ROLL_OFF).stream().limit(4)
                .filter(e -> e.colour() == Colour.RED).count(), "applied once");
    }

    @Test @DisplayName("a person who does not roll in time is rolled for, and that is not counted as a missed turn")
    void aTimeoutRollsForThePersonWithoutCountingAsAMiss() {
        kit.config = com.ludot.server.app.SessionConfig.defaults().withDelays(0, 1, 1).withTurnTimeoutMs(NEVER_MS);
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        startWithAliceAsRed(room, alice);
        ServerMessage.YourTurn offer = firstOffer(alice);

        room.injectTimeoutForTest(offer.turnNumber());
        awaitOrderAnnounced(alice);
        assertEquals(Colour.RED, eventsOfType(alice, EventType.ROLL_OFF).get(0).colour(), "RED was rolled for");

        // her first real turn: one miss is not enough for 'away' because the roll-off timeout did not count
        ServerMessage.YourTurn turn = Kit.sink(alice).await(ServerMessage.YourTurn.class,
                t -> t.phase() != Phase.ROLL_OFF, 20_000);
        room.injectTimeoutForTest(turn.turnNumber());
        Kit.settle(room, alice);
        assertTrue(Kit.sink(alice).of(ServerMessage.SeatsUpdated.class).stream().noneMatch(
                u -> u.seats().stream().anyMatch(x -> x.controller() == Controller.AFK)));
    }

    @Test @DisplayName("pausing freezes the roll-off, and resuming lets the AI seats carry on")
    void pauseFreezesTheRollOff() {
        kit.config = com.ludot.server.app.SessionConfig.defaults().withDelays(250, 250, 250).withTurnTimeoutMs(NEVER_MS);
        Participant alice = Kit.person("alice");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        startWithAliceAsRed(room, alice);
        ServerMessage.YourTurn offer = firstOffer(alice);
        Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), offer.turnNumber()));
        assertTrue(Kit.call(room, alice, new ClientMessage.PauseGame(id(), room.roomId())).isOk());

        int frozen = eventsOfType(alice, EventType.ROLL_OFF).size();
        Kit.sleep(800);
        assertEquals(frozen, eventsOfType(alice, EventType.ROLL_OFF).size(), "no AI roll while paused");

        assertTrue(Kit.call(room, alice, new ClientMessage.ResumeGame(id(), room.roomId())).isOk());
        awaitOrderAnnounced(alice);
    }

    @Test @DisplayName("a person who leaves while they are the next to roll is replaced by the AI, so the roll-off does not stall")
    void aLeaverDoesNotStallTheRollOff() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        Kit.call(room, bob, Kit.join(room));
        startWithAliceAsRed(room, alice);
        firstOffer(alice);

        assertTrue(Kit.call(room, alice, new ClientMessage.LeaveRoom(id(), room.roomId())).isOk());

        awaitOrderAnnounced(bob);
        assertEquals(Colour.RED, eventsOfType(bob, EventType.ROLL_OFF).get(0).colour());
    }

    @Test @DisplayName("someone joining mid roll-off sees whose roll it is; the play order appears once it is decided")
    void snapshotsDuringAndAfterTheRollOff() {
        Participant alice = Kit.person("alice");
        Participant bob = Kit.person("bob");
        GameSession room = kit.roomWith(RoomMode.PLAY, 2069, alice);
        startWithAliceAsRed(room, alice);
        ServerMessage.YourTurn offer = firstOffer(alice);

        Kit.call(room, bob, Kit.join(room));
        ServerMessage.RoomSnapshot during = Kit.sink(bob).await(ServerMessage.RoomSnapshot.class, 5000);
        assertEquals(Phase.ROLL_OFF, during.turn().phase());
        assertEquals(Colour.RED, during.turn().colour());
        assertTrue(during.playOrder().isEmpty());

        Kit.call(room, alice, new ClientMessage.Roll(id(), room.roomId(), offer.turnNumber()));
        awaitOrderAnnounced(alice);
        Kit.call(room, bob, new ClientMessage.RequestSnapshot(id(), room.roomId()));
        ServerMessage.RoomSnapshot after = Kit.sink(bob).await(ServerMessage.RoomSnapshot.class,
                s -> !s.playOrder().isEmpty(), 5000);
        assertEquals(4, after.playOrder().size());
    }

    @Test @DisplayName("in a bot match the four seats roll for the order by themselves, in seat order")
    void aBotMatchRollsForTheOrderOnItsOwn() {
        Participant host = Kit.person("host");
        GameSession room = kit.roomWith(RoomMode.BOT_MATCH, 1, host);
        awaitOrderAnnounced(host);
        assertEquals(List.of(Colour.RED, Colour.GREEN, Colour.YELLOW, Colour.BLUE),
                eventsOfType(host, EventType.ROLL_OFF).stream().limit(4).map(EventDto::colour).toList());
        assertTrue(Kit.sink(host).of(ServerMessage.YourTurn.class).isEmpty(), "nobody is asked to do anything");
        assertEquals(room.roomId(), room.summary().roomId());
    }
}
