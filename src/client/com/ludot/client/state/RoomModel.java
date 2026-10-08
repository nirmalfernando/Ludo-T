package com.ludot.client.state;

import com.ludot.protocol.BoardDto;
import com.ludot.protocol.EventDto;
import com.ludot.protocol.EventType;
import com.ludot.protocol.MemberDto;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.SeatDto;
import com.ludot.protocol.ServerMessage;
import com.ludot.protocol.ServerMessage.ChatMessage;
import com.ludot.protocol.Speed;
import com.ludot.protocol.TurnInfo;
import com.ludot.protocol.Variant;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Everything the client shows about the room it is in, as JavaFX properties the views bind to. The
 * client never works game state out itself: each update carries the whole board, so a missed one is
 * repaired by the next. It only checks that room pushes arrive in sequence and asks for a fresh
 * snapshot if one is missing. Touch it only from the FX application thread.
 */
public final class RoomModel {

    private static final int LOG_LIMIT = 400;
    private static final int CHAT_LIMIT = 300;

    private final String roomId;
    private final String myId;
    private final Runnable onGap;

    private final ObjectProperty<RoomMode> mode = new SimpleObjectProperty<>();
    private final ObjectProperty<Variant> variant = new SimpleObjectProperty<>();
    private final ObjectProperty<RoomStatus> status = new SimpleObjectProperty<>(RoomStatus.WAITING);
    private final ObjectProperty<Speed> speed = new SimpleObjectProperty<>(Speed.NORMAL);
    private final StringProperty hostId = new SimpleStringProperty("");
    private final ObjectProperty<BoardDto> board = new SimpleObjectProperty<>();
    private final ObjectProperty<TurnInfo> turn = new SimpleObjectProperty<>();
    private final ObjectProperty<ServerMessage.YourTurn> myTurn = new SimpleObjectProperty<>();
    private final ObjectProperty<List<EventDto>> latestEvents = new SimpleObjectProperty<>(List.of());
    private final ObjectProperty<List<com.ludot.protocol.Colour>> standings = new SimpleObjectProperty<>();
    private final ObjectProperty<List<com.ludot.protocol.Colour>> playOrder = new SimpleObjectProperty<>(List.of());
    private final LongProperty stateHash = new SimpleLongProperty();
    private final LongProperty seq = new SimpleLongProperty();
    private final BooleanProperty seeded = new SimpleBooleanProperty();
    private final ObservableList<SeatDto> seats = FXCollections.observableArrayList();
    private final ObservableList<MemberDto> members = FXCollections.observableArrayList();
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ObservableList<LogLine> logLines = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> chat = FXCollections.observableArrayList();

    /**
     * @param roomId the room
     * @param myId   this client's player id, to know which seat and which turn are ours
     * @param onGap  called when a push is missing, so the owner can request a fresh snapshot
     */
    public RoomModel(String roomId, String myId, Runnable onGap) {
        this.roomId = roomId;
        this.myId = myId;
        this.onGap = onGap;
    }

    public String roomId() {
        return roomId;
    }

    /** Applies a message addressed to this room; pushes out of sequence trigger a resync instead. */
    public void apply(ServerMessage message) {
        if (message instanceof ServerMessage.RoomSnapshot snapshot) {
            applySnapshot(snapshot);
        } else if (message instanceof ServerMessage.YourTurn turnOffer) {
            myTurn.set(turnOffer);
        } else if (message instanceof ServerMessage.RoomPush push) {
            applyPush(push);
        }
    }

    private void applySnapshot(ServerMessage.RoomSnapshot s) {
        mode.set(s.mode());
        variant.set(s.variant());
        status.set(s.status());
        speed.set(s.speed());
        hostId.set(s.hostId());
        board.set(s.board());
        turn.set(s.turn());
        playOrder.set(s.playOrder());
        stateHash.set(s.stateHash());
        seats.setAll(s.seats());
        members.setAll(s.members());
        log.setAll(s.recentLog());
        logLines.setAll(s.recentLog().stream().map(text -> new LogLine(null, text)).toList());
        chat.setAll(s.recentChat());
        seq.set(s.seq());
        seeded.set(true);
    }

    private void applyPush(ServerMessage.RoomPush push) {
        if (!seeded.get() || push.seq() <= seq.get()) {
            return; // not seeded yet, or already covered by the snapshot
        }
        if (push.seq() != seq.get() + 1) {
            onGap.run();
            return;
        }
        seq.set(push.seq());
        if (push instanceof ServerMessage.StateUpdate update) {
            applyState(update);
        } else if (push instanceof ServerMessage.SeatsUpdated updated) {
            seats.setAll(updated.seats());
        } else if (push instanceof ServerMessage.MembersChanged changed) {
            members.setAll(changed.members());
        } else if (push instanceof ServerMessage.RoomStateChanged changed) {
            status.set(changed.status());
            speed.set(changed.speed());
            hostId.set(changed.hostId());
        } else if (push instanceof ServerMessage.GameOver over) {
            standings.set(over.standings());
        } else if (push instanceof ServerMessage.ChatMessage message) {
            chat.add(message);
            trim(chat, CHAT_LIMIT);
        }
    }

    private void applyState(ServerMessage.StateUpdate update) {
        board.set(update.board());
        turn.set(update.turn());
        stateHash.set(update.stateHash());
        latestEvents.set(update.events());
        for (EventDto event : update.events()) {
            if (event.type() == EventType.TURN_ORDER && event.colour() != null) {
                playOrder.set(clockwiseFrom(event.colour())); // the roll-off is over: play goes clockwise from the winner
            }
            if (!event.text().isBlank()) {
                log.add(event.text());
                logLines.add(new LogLine(event.type(), event.text()));
            }
        }
        trim(log, LOG_LIMIT);
        trim(logLines, LOG_LIMIT);
        ServerMessage.YourTurn offered = myTurn.get();
        if (offered != null && offered.turnNumber() < update.turn().turnNumber()) {
            myTurn.set(null); // the game has moved past the offer
        }
    }

    private static void trim(ObservableList<?> list, int limit) {
        if (list.size() > limit) {
            list.remove(0, list.size() - limit);
        }
    }

    // ------------------------------------------------------------------ views read these

    public boolean isHost() {
        return myId.equals(hostId.get());
    }

    public java.util.Optional<SeatDto> mySeat() {
        return seats.stream().filter(seat -> myId.equals(seat.playerId())).findFirst();
    }

    public ObjectProperty<RoomMode> modeProperty() {
        return mode;
    }

    public ObjectProperty<Variant> variantProperty() {
        return variant;
    }

    public ObjectProperty<RoomStatus> statusProperty() {
        return status;
    }

    public ObjectProperty<Speed> speedProperty() {
        return speed;
    }

    public StringProperty hostIdProperty() {
        return hostId;
    }

    public ObjectProperty<BoardDto> boardProperty() {
        return board;
    }

    public ObjectProperty<TurnInfo> turnProperty() {
        return turn;
    }

    public ObjectProperty<ServerMessage.YourTurn> myTurnProperty() {
        return myTurn;
    }

    public ObjectProperty<List<EventDto>> latestEventsProperty() {
        return latestEvents;
    }

    public ObjectProperty<List<com.ludot.protocol.Colour>> standingsProperty() {
        return standings;
    }

    public LongProperty stateHashProperty() {
        return stateHash;
    }

    /** The colours in playing order when {@code first} begins: clockwise, which is the order R, G, Y, B. */
    public static List<com.ludot.protocol.Colour> clockwiseFrom(com.ludot.protocol.Colour first) {
        com.ludot.protocol.Colour[] seats = com.ludot.protocol.Colour.values();
        List<com.ludot.protocol.Colour> order = new java.util.ArrayList<>();
        for (int i = 0; i < seats.length; i++) {
            order.add(seats[(first.ordinal() + i) % seats.length]);
        }
        return List.copyOf(order);
    }

    /** The colours in playing order: empty until the opening roll-off has decided it. */
    public ObjectProperty<List<com.ludot.protocol.Colour>> playOrderProperty() {
        return playOrder;
    }

    public LongProperty seqProperty() {
        return seq;
    }

    public ObservableList<SeatDto> seats() {
        return seats;
    }

    public ObservableList<MemberDto> members() {
        return members;
    }

    /** The same lines as {@link #log()}, with each live event's type so the event log can be filtered. */
    public ObservableList<LogLine> logLines() {
        return logLines;
    }

    public ObservableList<String> log() {
        return log;
    }

    public ObservableList<ChatMessage> chat() {
        return chat;
    }
}
