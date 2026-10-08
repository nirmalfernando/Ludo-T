package com.ludot.protocol;

import java.util.List;

public record FinishedGameList(List<FinishedGame> games, int page, boolean hasMore) implements java.io.Serializable {

    public FinishedGameList {
        games = List.copyOf(games);
    }
}
