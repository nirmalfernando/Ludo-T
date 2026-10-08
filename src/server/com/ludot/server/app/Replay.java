package com.ludot.server.app;

import com.ludot.protocol.ReplayEndReason;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.Speed;
import com.ludot.server.game.RoomGame;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

final class Replay {

    private final String id;
    private final ReplayData data;
    private final ClientSink sink;
    private final ScheduledExecutorService scheduler;
    private final Function<Speed, Long> delayFor;
    private final Consumer<Replay> onEnded;
    private final RoomGame game;
    private Speed speed;
    private int next; // index of the next decision to apply
    private boolean paused;
    private boolean ended;
    private ScheduledFuture<?> pending;

    Replay(String id, ReplayData data, ClientSink sink, Speed speed, ScheduledExecutorService scheduler,
           Function<Speed, Long> delayFor, Consumer<Replay> onEnded) {
        this.id = id;
        this.data = data;
        this.sink = sink;
        this.speed = speed;
        this.scheduler = scheduler;
        this.delayFor = delayFor;
        this.onEnded = onEnded;
        this.game = new RoomGame(data.variant(), data.seed());
    }

    String id() {
        return id;
    }

    synchronized void begin() {
        if (ended) {
            return;
        }
        push(game.open().events()); // the roll-off rolls are recorded steps like any other, so they replay one by one
        scheduleNext();
    }

    synchronized void pause() {
        if (!ended && !paused) {
            paused = true;
            cancelPending();
        }
    }

    synchronized void resume() {
        if (!ended && paused) {
            paused = false;
            scheduleNext();
        }
    }

    synchronized void setSpeed(Speed newSpeed) {
        speed = newSpeed;
        if (!ended && !paused) {
            cancelPending();
            scheduleNext();
        }
    }

    synchronized void stop() {
        end(ReplayEndReason.STOPPED);
    }

    private void scheduleNext() {
        pending = scheduler.schedule(this::tick, delayFor.apply(speed), TimeUnit.MILLISECONDS);
    }

    private synchronized void tick() {
        if (ended || paused) {
            return;
        }
        RoomGame.Step result = applyNext();
        if (result == null) {
            return; // diverged: already ended
        }
        next++;
        push(result.events());
        if (next >= data.steps().size()) {
            end(game.isFinished() ? ReplayEndReason.COMPLETED : ReplayEndReason.DIVERGED);
        } else if (!ended) {
            scheduleNext();
        }
    }

    // plays the next stored decision; null (after ending as DIVERGED) if the engine disagrees with the record
    private RoomGame.Step applyNext() {
        ReplayData.Step decision = data.steps().get(next);
        try {
            RoomGame.Step result = decision.optionIndex() != null ? game.applyMove(decision.colour(), decision.optionIndex())
                    : game.phase() == com.ludot.protocol.Phase.ROLL_OFF ? game.rollOff(decision.colour())
                    : game.roll(decision.colour());
            if (rollDiffers(decision, result)) {
                end(ReplayEndReason.DIVERGED); // the same seed must give the same dice
                return null;
            }
            return result;
        } catch (RuntimeException e) {
            end(ReplayEndReason.DIVERGED); // the engine refused a decision it once accepted
            return null;
        }
    }

    private static boolean rollDiffers(ReplayData.Step decision, RoomGame.Step result) {
        return decision.optionIndex() == null && decision.roll() != null && !decision.roll().equals(result.rolled());
    }

    // a viewer that cannot keep up simply stops the replay: nothing else depends on it
    private void push(List<com.ludot.protocol.EventDto> events) {
        if (!sink.offer(new ServerMessage.ReplayEvent(id, next, data.steps().size(), events, game.board()))) {
            end(ReplayEndReason.STOPPED);
        }
    }

    private void end(ReplayEndReason reason) {
        if (ended) {
            return;
        }
        ended = true;
        cancelPending();
        sink.offer(new ServerMessage.ReplayEnded(id, reason));
        onEnded.accept(this);
    }

    private void cancelPending() {
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
    }
}
