package com.ludot.client.state;

import com.ludot.protocol.BoardDto;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.FinishedGame;
import com.ludot.protocol.ReplayEndReason;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.Speed;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class ReplayModel {

    private static final int LOG_LIMIT = 400;

    private final String replayId;
    private final FinishedGame game;
    private final ObjectProperty<BoardDto> board = new SimpleObjectProperty<>();
    private final ObjectProperty<List<EventDto>> latestEvents = new SimpleObjectProperty<>(List.of());
    private final IntegerProperty index = new SimpleIntegerProperty();
    private final IntegerProperty total = new SimpleIntegerProperty();
    private final ObjectProperty<ReplayEndReason> ended = new SimpleObjectProperty<>();
    private final BooleanProperty paused = new SimpleBooleanProperty();
    private final ObjectProperty<Speed> speed;
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ObservableList<LogLine> logLines = FXCollections.observableArrayList();

    public ReplayModel(String replayId, FinishedGame game, Speed speed) {
        this.replayId = replayId;
        this.game = game;
        this.speed = new SimpleObjectProperty<>(speed);
        this.total.set(Math.max(game.turns(), 0));
    }

    public void apply(ServerMessage.ReplayEvent step) {
        if (!replayId.equals(step.replayId())) {
            return;
        }
        board.set(step.board());
        index.set(step.index());
        total.set(step.total());
        latestEvents.set(step.events());
        for (EventDto event : step.events()) {
            if (!event.text().isBlank()) {
                log.add(event.text());
                logLines.add(new LogLine(event.type(), event.text()));
            }
        }
        trim(log);
        trim(logLines);
    }

    public void apply(ServerMessage.ReplayEnded finish) {
        if (replayId.equals(finish.replayId())) {
            ended.set(finish.reason());
        }
    }

    private static void trim(ObservableList<?> list) {
        if (list.size() > LOG_LIMIT) {
            list.remove(0, list.size() - LOG_LIMIT);
        }
    }

    public String replayId() {
        return replayId;
    }

    public FinishedGame game() {
        return game;
    }

    public ObjectProperty<BoardDto> boardProperty() {
        return board;
    }

    public ObjectProperty<List<EventDto>> latestEventsProperty() {
        return latestEvents;
    }

    public IntegerProperty indexProperty() {
        return index;
    }

    public IntegerProperty totalProperty() {
        return total;
    }

    public ObjectProperty<ReplayEndReason> endedProperty() {
        return ended;
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }

    public ObjectProperty<Speed> speedProperty() {
        return speed;
    }

    public ObservableList<String> log() {
        return log;
    }

    public ObservableList<LogLine> logLines() {
        return logLines;
    }
}
