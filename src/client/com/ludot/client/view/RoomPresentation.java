package com.ludot.client.view;

import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.RoomSummary;
import com.ludot.protocol.Variant;
import java.util.ArrayList;
import java.util.List;

// no JavaFX, so it is easy to test
public final class RoomPresentation {

    public enum Action { JOIN, WATCH }

    public enum SeatState { PERSON, AI, FREE }

    private static final int SEATS = 4;

    private RoomPresentation() {
    }

    public static String statusLabel(RoomStatus status) {
        return status.name();
    }

    public static String statusColour(RoomStatus status) {
        return switch (status) {
            case WAITING -> "#2eb872";
            case RUNNING -> "#ff9f1c";
            case PAUSED -> "#e6b800";
            case FINISHED, CLOSED -> "#9aa3c7";
        };
    }

    public static String modeLabel(RoomMode mode) {
        return mode == RoomMode.BOT_MATCH ? "BOT MATCH" : "PLAY";
    }

    public static String rulesLabel(Variant variant) {
        return variant == Variant.LUDO_T ? "LUDO-T" : "CLASSIC";
    }

    public static Action action(RoomSummary room) {
        boolean joinable = room.mode() == RoomMode.PLAY && room.status() == RoomStatus.WAITING;
        return joinable ? Action.JOIN : Action.WATCH;
    }

    public static String actionLabel(Action action) {
        return action == Action.JOIN ? "JOIN" : "WATCH";
    }

    public static List<SeatState> seats(RoomSummary room) {
        List<SeatState> seats = new ArrayList<>(SEATS);
        for (int i = 0; i < SEATS; i++) {
            if (i < room.humanSeats()) {
                seats.add(SeatState.PERSON);
            } else if (i < room.humanSeats() + room.aiSeats()) {
                seats.add(SeatState.AI);
            } else {
                seats.add(SeatState.FREE);
            }
        }
        return seats;
    }

    public static String peopleText(RoomSummary room) {
        String people = room.humanSeats() == 1 ? "1 Person" : room.humanSeats() + " People";
        return people + ", " + room.aiSeats() + " AI";
    }

    public static String watchersText(int spectators) {
        return spectators == 0 ? "Nobody Watching" : spectators + " Watching";
    }
}
