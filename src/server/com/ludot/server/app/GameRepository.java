package com.ludot.server.app;

/**
 * Port for persistence. Implementations must not block the caller for long: the room thread calls
 * these inline, so real databases are reached through an asynchronous decorator.
 */
public interface GameRepository {

    GameRepository NONE = new GameRepository() {
        @Override
        public void recordDecision(DecisionRecord decision) {
            // persistence disabled
        }

        @Override
        public void recordChat(ChatRecord chat) {
            // persistence disabled
        }
    };

    void recordDecision(DecisionRecord decision);

    void recordChat(ChatRecord chat);
}
