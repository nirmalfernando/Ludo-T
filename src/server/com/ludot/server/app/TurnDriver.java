package com.ludot.server.app;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.ServerMessage;
import com.ludot.server.game.RoomGame;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Decides, after every step of the game, what happens next and sets the clocks: a person gets a deadline, a
 * seat played by the AI gets an AI action after the pacing delay, and the game ends when it is over. Every
 * timer only submits a command to the room's queue carrying the turn it was set for, so a late timer finds a
 * different turn and does nothing; no timer ever touches room state itself.
 */
final class TurnDriver {

    private final RoomState state;
    private final Broadcaster broadcaster;
    private final RoomChat chat;
    private final RoomActor actor;
    private final GameRepository repository;
    private final Map<Colour, Integer> missedTurns = new EnumMap<>(Colour.class);
    private volatile ScheduledFuture<?> pendingAi;          // read by shutdown() from another thread
    private volatile ScheduledFuture<?> pendingDeadline;

    TurnDriver(RoomState state, Broadcaster broadcaster, RoomChat chat, RoomActor actor, GameRepository repository) {
        this.state = state;
        this.broadcaster = broadcaster;
        this.chat = chat;
        this.actor = actor;
        this.repository = repository;
    }

    // ------------------------------------------------------------------ after a step

    /** Called after every roll or move: arms the next clock, tells everyone, then announces the next turn or the end. */
    void afterStep(RoomGame.Step step) {
        arm(); // decides the next deadline first, so the update can carry it to every client
        broadcaster.stateUpdate(step);
        remember(step);
        if (state.game.isFinished()) {
            finishGame();
        } else {
            announce();
        }
    }

    private void remember(RoomGame.Step step) {
        step.events().stream().map(e -> e.text()).filter(t -> !t.isBlank()).forEach(state.log::addLast);
        while (state.log.size() > state.config.recentLogSize()) {
            state.log.removeFirst();
        }
    }

    private void finishGame() {
        state.status = RoomStatus.FINISHED;
        cancelTimers();
        List<Colour> standings = state.game.standings();
        repository.record(new RoomStatusRecord(state.roomId, RoomStatus.FINISHED, standings.get(0),
                state.game.turnNumber()));
        broadcaster.roomState();
        broadcaster.broadcast(s -> new ServerMessage.GameOver(state.roomId, s, standings, state.roomId));
        chat.postSystem(standings.get(0) + " wins!");
        broadcaster.publishSummary();
    }

    // ------------------------------------------------------------------ arming the clocks

    /** Arms the clocks for whoever acts next and tells a person it is their turn. */
    void scheduleNext() {
        arm();
        announce();
    }

    /**
     * Sets the clocks. A person gets a deadline; a seat played by the AI gets an AI action after the pacing
     * delay. Nothing is sent here: the caller announces the turn once it has broadcast the update that
     * carries the deadline.
     */
    void arm() {
        cancelTimers();
        if (state.status != RoomStatus.RUNNING || state.game.isFinished()) {
            return;
        }
        Controller controller = state.seats.seat(state.game.currentColour()).controller();
        int expectedTurn = state.game.turnNumber();
        if (controller == Controller.HUMAN && !nothingToDecide()) {
            armDeadline(expectedTurn);
            return;
        }
        pendingAi = actor.schedule(() -> actor.submitInternal(() -> aiAct(expectedTurn)),
                state.config.delayMs(state.speed));
    }

    private void armDeadline(int expectedTurn) {
        long timeout = state.config.turnTimeoutMs();
        if (timeout > 0) {
            state.deadline = state.clock.getAsLong() + timeout;
            pendingDeadline = actor.schedule(() -> actor.submitInternal(() -> onTurnTimeout(expectedTurn)), timeout);
        }
    }

    /** Tells the person whose turn it is, privately, what they may do. */
    void announce() {
        if (state.status != RoomStatus.RUNNING || state.game.isFinished()) {
            return;
        }
        SeatTable.Seat seat = state.seats.seat(state.game.currentColour());
        if (seat.controller() == Controller.HUMAN && !nothingToDecide()) {
            Member member = state.members.get(seat.playerId());
            if (member != null) {
                broadcaster.deliver(member, new ServerMessage.YourTurn(state.roomId, state.game.turnNumber(),
                        state.game.phase(), state.game.lastRoll(), state.game.options(), state.deadline));
            }
        }
    }

    // nothing for a person to decide: the only legal move is to skip, so the room applies it after the pacing delay
    private boolean nothingToDecide() {
        return state.game.phase() == Phase.AWAITING_MOVE && state.game.onlySkipAvailable();
    }

    /** Stops every clock and forgets the deadline (the game is paused, finished or being shut down). */
    void cancelTimers() {
        RoomActor.cancel(pendingAi);
        RoomActor.cancel(pendingDeadline);
        state.deadline = null;
    }

    private boolean aiPending() {
        ScheduledFuture<?> pending = pendingAi;
        return pending != null && !pending.isDone();
    }

    /** After a speed change: re-times a pending AI action at the new speed; a person's deadline is untouched. */
    void retimeAi() {
        if (aiPending()) {
            arm();
        }
    }

    /** After a seat was handed to the AI: if that seat is the one to act and nothing is pending, the AI takes over. */
    void takeOverIfNeeded() {
        if (state.game.isStarted()
                && state.seats.seat(state.game.currentColour()).controller() != Controller.HUMAN && !aiPending()) {
            scheduleNext();
        }
    }

    // ------------------------------------------------------------------ acting for a seat

    /** A person acted (or took their seat back): their run of missed turns is over. */
    void resetMisses(Colour colour) {
        missedTurns.remove(colour);
    }

    void record(int turnNumber, Colour colour, Integer roll, Integer optionIndex, DecisionSource source) {
        repository.record(new DecisionRecord(state.roomId, turnNumber, colour, roll, optionIndex, source));
    }

    /**
     * The deadline passed. Like every other action this is a queued command carrying the turn it was set
     * for, so if the person acted first (or the game moved on for any reason) it finds a different turn and
     * does nothing. Otherwise the seat's own strategy completes the phase, and a second miss in a row hands
     * the seat to the AI until the person takes it back.
     */
    void onTurnTimeout(int expectedTurn) {
        if (!stillWaitingFor(expectedTurn)) {
            return;
        }
        Colour colour = state.game.currentColour();
        SeatTable.Seat seat = state.seats.seat(colour);
        if (seat.controller() != Controller.HUMAN) {
            return;
        }
        boolean rollOff = state.game.phase() == Phase.ROLL_OFF; // rolled for, but not a missed turn: nothing was at stake
        int missed = rollOff ? 0 : missedTurns.merge(colour, 1, Integer::sum);
        RoomGame.Step step = actFor(colour, expectedTurn, DecisionSource.TIMEOUT, false);
        if (!rollOff && missed >= state.config.afkAfterTimeouts()) {
            state.seats.setController(colour, Controller.AFK);
            broadcaster.seats();
            chat.postSystem(seat.nickname() + " is away: the AI plays " + colour + " until they take it back");
        }
        afterStep(step);
    }

    /** The pacing delay passed for a seat the AI plays (or for a person with nothing to decide). */
    private void aiAct(int expectedTurn) {
        if (!stillWaitingFor(expectedTurn)) {
            return;
        }
        Colour colour = state.game.currentColour();
        boolean human = state.seats.seat(colour).controller() == Controller.HUMAN;
        boolean forcedSkip = human && nothingToDecide();
        if (human && !forcedSkip) {
            return; // a person is deciding; their own request or their deadline will move the game on
        }
        afterStep(actFor(colour, expectedTurn, forcedSkip ? DecisionSource.AUTO_SKIP : DecisionSource.AI, forcedSkip));
    }

    private boolean stillWaitingFor(int turn) {
        return state.status == RoomStatus.RUNNING && !state.game.isFinished() && state.game.turnNumber() == turn;
    }

    // the roll-off roll or the roll if one is due, otherwise the move the seat's own strategy picks (or the skip, when forced)
    private RoomGame.Step actFor(Colour colour, int turn, DecisionSource source, boolean takeFirstOption) {
        Phase phase = state.game.phase();
        if (phase == Phase.ROLL_OFF || phase == Phase.AWAITING_ROLL) {
            RoomGame.Step step = phase == Phase.ROLL_OFF ? state.game.rollOff(colour) : state.game.roll(colour);
            record(turn, colour, step.rolled(), null, source);
            return step;
        }
        int choice = takeFirstOption ? 0 : state.game.strategyChoice();
        Integer rolled = state.game.lastRoll();
        RoomGame.Step step = state.game.applyMove(colour, choice);
        record(turn, colour, rolled, choice, source);
        return step;
    }
}
