package com.ludot.server.app;

import com.ludot.protocol.ServerMessage.Response;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// remembers the most recent responses per player so a repeated request id gets the original answer again instead of being applied twice
final class RequestLog {

    private final int perPlayer;
    private final Map<String, Map<UUID, Response>> byPlayer = new HashMap<>();

    RequestLog(int perPlayer) {
        this.perPlayer = perPlayer;
    }

    Response find(String playerId, UUID requestId) {
        Map<UUID, Response> responses = byPlayer.get(playerId);
        return responses == null ? null : responses.get(requestId);
    }

    void remember(String playerId, Response response) {
        byPlayer.computeIfAbsent(playerId, id -> lruMap()).put(response.requestId(), response);
    }

    private Map<UUID, Response> lruMap() {
        return new LinkedHashMap<>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Response> eldest) {
                return size() > perPlayer;
            }
        };
    }
}
