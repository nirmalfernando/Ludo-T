package com.ludot.server.app;

import com.ludot.protocol.ServerMessage.Response;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers the most recent responses per player so a repeated request id gets the original answer
 * again instead of being applied twice. Bounded per player (least recently used entries go first).
 * Confined to the room thread.
 */
final class RequestLog {

    private final int perPlayer;
    private final Map<String, Map<UUID, Response>> byPlayer = new HashMap<>();

    RequestLog(int perPlayer) {
        this.perPlayer = perPlayer;
    }

    /** The earlier response to this request id, or null if it is new. */
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
