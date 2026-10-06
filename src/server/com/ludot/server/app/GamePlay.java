package com.ludot.server.app;

import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Controller;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.server.game.RoomGame;
import java.util.Map;

/** The game commands: start, roll, choose a move, and the host's pause, resume and speed controls. */
final class GamePlay {

    private final RoomState state;
    private final RoomGuards guards;
    private final Broadcaster broadcaster;
    private final RoomChat chat;
    private final TurnDriver turns;
    private final GameRepository repository;

    GamePlay(RoomState state, RoomGuards guards, Broadcaster broadcaster, RoomChat chat, TurnDriver turns,
             GameRepository repository) {
        this.state = state;
        this.guards = guards;
        this.broadcaster = broadcaster;
        this.chat = chat;
        this.turns = turns;
        this.repository = repository;
    }

    void register(Map<Class<?>, RequestHandler> handlers) {
        handlers.put(ClientMessage.StartGame.class, this::startGame);
        handlers.put(ClientMessage.Roll.class, (from, request) -> roll(from, (ClientMessage.Roll) request));
        handlers.put(ClientMessage.ChooseMove.class, (from, request) -> chooseMove(from, (ClientMessage.ChooseMove) request));
        handlers.put(ClientMessage.PauseGame.class, this::pauseGame);
        handlers.put(ClientMessage.ResumeGame.class, this::resumeGame);
        handlers.put(ClientMessage.SetSpeed.class, (from, request) -> setSpeed(from, (ClientMessage.SetSpeed) request));
    }

    // ------------------------------------------------------------------ starting

    private Reply startGame(Participant from, ClientMessage.Request request) {
        guards.requireHost(from);
        if (state.mode != RoomMode.PLAY || state.status != RoomStatus.WAITING) {
            throw RoomGuards.reject(ErrorCode.SEATS_LOCKED, "The game has already started");
        }
        if (state.seats.humanSeats() == 0) {
            throw RoomGuards.reject(ErrorCode.NO_HUMAN_SEATS, "At least one seat must be taken by a person");
        }
        return Reply.ok(request).then(() -> {
            beginGame();
            broadcaster.publishSummary();
        });
    }

    /** Hands empty seats to the AI, runs the roll-off and opens the first turn. */
    void beginGame() {
        state.seats.fillFreeSeatsWithAi();
        state.status = RoomStatus.RUNNING;
        repository.record(new SeatsRecord(state.roomId, state.seats.rows()));
        repository.record(new RoomStatusRecord(state.roomId, RoomStatus.RUNNING, null, 0));
        RoomGame.Step step = state.game.start();
        broadcaster.seats();
        broadcaster.roomState();
        chat.postSystem("The game has started");
        turns.afterStep(step);
    }

    // ------------------------------------------------------------------ a person's turn

    private Reply roll(Participant from, ClientMessage.Roll request) {
        requireTurn(from, request.turnNumber(), Phase.AWAITING_ROLL);
        Colour colour = state.game.currentColour();
        turns.resetMisses(colour);
        RoomGame.Step step = state.game.roll(colour);
        turns.record(request.turnNumber(), colour, step.rolled(), null, DecisionSource.HUMAN);
        return Reply.ok(request, step.rolled()).then(() -> turns.afterStep(step));
    }

    private Reply chooseMove(Participant from, ClientMessage.ChooseMove request) {
        requireTurn(from, request.turnNumber(), Phase.AWAITING_MOVE);
        int index = request.optionIndex();
        if (index < 0 || index >= state.game.options().size()) {
            throw RoomGuards.reject(ErrorCode.INVALID_MOVE, "No such move: " + index);
        }
        Colour colour = state.game.currentColour();
        turns.resetMisses(colour);
        Integer rolled = state.game.lastRoll();
        RoomGame.Step step = state.game.applyMove(colour, index);
        turns.record(request.turnNumber(), colour, rolled, index, DecisionSource.HUMAN);
        return Reply.ok(request).then(() -> turns.afterStep(step));
    }

    // validation order: member, game state, whose turn, which turn, which phase
    private void requireTurn(Participant from, int turnNumber, Phase expected) {
        Member member = guards.requireMember(from);
        guards.requireRunning();
        SeatTable.Seat active = state.seats.seat(state.game.currentColour());
        if (state.seats.seatOf(member.id()).isEmpty()) {
            throw RoomGuards.reject(ErrorCode.NOT_A_PLAYER, "Spectators cannot play");
        }
        if (active.controller() != Controller.HUMAN || !active.heldBy(member.id())) {
            throw RoomGuards.reject(ErrorCode.NOT_YOUR_TURN, "It is " + active.colour() + "'s turn");
        }
        if (turnNumber != state.game.turnNumber()) {
            throw RoomGuards.reject(ErrorCode.STALE_TURN,
                    "Turn " + turnNumber + " is over; now at " + state.game.turnNumber());
        }
        if (state.game.phase() != expected) {
            throw RoomGuards.reject(ErrorCode.WRONG_PHASE, "Expected " + state.game.phase());
        }
    }

    // ------------------------------------------------------------------ host controls

    private Reply pauseGame(Participant from, ClientMessage.Request request) {
        guards.requireHost(from);
        guards.requireRunning();
        state.status = RoomStatus.PAUSED;
        turns.cancelTimers();
        return Reply.ok(request).then(() -> {
            broadcaster.roomState();
            chat.postSystem("The host paused the game");
            broadcaster.publishSummary();
        });
    }

    private Reply resumeGame(Participant from, ClientMessage.Request request) {
        guards.requireHost(from);
        if (state.status != RoomStatus.PAUSED) {
            guards.requireRunning();
            throw RoomGuards.reject(ErrorCode.GAME_NOT_RUNNING, "The game is not paused");
        }
        state.status = RoomStatus.RUNNING;
        state.autoPaused = false;
        return Reply.ok(request).then(() -> {
            broadcaster.roomState();
            chat.postSystem("The host resumed the game");
            turns.scheduleNext();
            broadcaster.turnRefresh();
            broadcaster.publishSummary();
        });
    }

    private Reply setSpeed(Participant from, ClientMessage.SetSpeed request) {
        guards.requireHost(from);
        state.speed = request.speed();
        return Reply.ok(request).then(() -> {
            broadcaster.roomState();
            turns.retimeAi();
        });
    }
}
