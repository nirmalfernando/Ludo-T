package com.ludot.server.app;

import com.ludot.protocol.FinishedGameList;
import java.util.List;
import java.util.Optional;

public interface GameHistory {

    int PAGE_SIZE = 8;

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

    FinishedGameList finished(int page);

    Optional<ReplayData> load(String gameId);
}
