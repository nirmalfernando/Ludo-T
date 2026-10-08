package com.ludot.client.view;

import com.ludot.client.feedback.Cue;
import com.ludot.client.feedback.Feedback;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.MemberDto;
import com.ludot.protocol.Phase;
import com.ludot.protocol.ServerMessage;
import javafx.collections.ListChangeListener;

/** The sounds that are about the room rather than the board: "your turn to roll" and "someone joined". */
final class RoomCues {

    private int lastCuedTurn = -1;
    private int memberCount;

    RoomCues(RoomModel model) {
        memberCount = model.members().size();
        model.myTurnProperty().addListener((obs, old, offer) -> yourTurn(offer));
        model.members().addListener((ListChangeListener<MemberDto>) change -> membersChanged(model.members().size()));
    }

    // once per turn: refreshing the screen for the same offer must not ping again
    private void yourTurn(ServerMessage.YourTurn offer) {
        if (offer != null && offer.phase() != Phase.AWAITING_MOVE && offer.turnNumber() != lastCuedTurn) {
            lastCuedTurn = offer.turnNumber();
            Feedback.get().play(Cue.YOUR_TURN);
        }
    }

    private void membersChanged(int now) {
        if (now > memberCount) {
            Feedback.get().play(Cue.JOIN);
        }
        memberCount = now;
    }
}
