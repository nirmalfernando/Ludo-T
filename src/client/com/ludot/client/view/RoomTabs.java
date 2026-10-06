package com.ludot.client.view;

import com.ludot.client.net.Session;
import com.ludot.client.state.RoomModel;
import com.ludot.protocol.ClientMessage;
import com.ludot.protocol.ErrorCode;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** The right-hand side of the room screen: room chat and the narrated event log. */
final class RoomTabs extends TabPane {

    private static final double WIDTH = 310;

    RoomTabs(RoomModel model, String myId, Session session) {
        ChatPane chat = new ChatPane(model.chat(), myId, (text, onRejected) -> session.request(
                new ClientMessage.SendChat(UUID.randomUUID(), model.roomId(), text), response -> {
                    if (!response.isOk()) {
                        onRejected.accept(response.errorCode() == ErrorCode.CHAT_RATE_LIMITED
                                ? "You are sending too fast: slow down" : Session.describe(response));
                    }
                }));
        getTabs().addAll(fixed("Chat", chat), fixed("Event Log", new EventLogPane(model.logLines())));
        setPrefWidth(WIDTH);
        setPadding(new Insets(0, 0, 0, 10));
    }

    private static Tab fixed(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}
