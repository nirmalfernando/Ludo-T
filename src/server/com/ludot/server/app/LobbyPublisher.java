package com.ludot.server.app;

import com.ludot.protocol.RoomSummary;

public interface LobbyPublisher {

    LobbyPublisher NONE = new LobbyPublisher() {
        @Override
        public void roomChanged(RoomSummary summary) {
            // nobody is listening
        }

        @Override
        public void roomClosed(String roomId) {
            // nobody is listening
        }
    };

    void roomChanged(RoomSummary summary);

    void roomClosed(String roomId);
}
