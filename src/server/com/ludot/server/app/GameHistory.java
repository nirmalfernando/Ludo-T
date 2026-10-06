package com.ludot.server.app;

import com.ludot.protocol.FinishedGameList;
import java.util.List;
import java.util.Optional;

/** The read side of storage: which games have finished, and what is needed to replay one. */
public interface GameHistory {

    int PAGE_SIZE = 8;

    /** With no storage there is nothing to list or replay. */
    GameHistory NONE = new GameHistory() {
        @Override
        public FinishedGameList finished(int page) {
            return new FinishedGameList(List.of(), Math.max(0, page), false);
        }

        @Override
        public Optional<ReplayData> load(String gameId) {
            return Optional.empty();
        }
    };

    /** One page of finished games, most recent first. */
    FinishedGameList finished(int page);

    /** The finished game with this id, or empty if there is none. */
    Optional<ReplayData> load(String gameId);
}
