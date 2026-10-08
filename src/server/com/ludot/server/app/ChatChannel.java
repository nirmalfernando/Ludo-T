package com.ludot.server.app;

import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.ServerMessage.ChatMessage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

public final class ChatChannel {

    private final int maxLength;
    private final int historySize;
    private final int rateLimitMessages;
    private final long rateLimitWindowMs;
    private final LongSupplier clock;
    private final Deque<ChatMessage> history = new ArrayDeque<>();
    private final Map<String, Deque<Long>> sendTimes = new HashMap<>();

    public ChatChannel(SessionConfig config, LongSupplier clock) {
        this.maxLength = config.chatMaxLength();
        this.historySize = config.chatHistorySize();
        this.rateLimitMessages = config.chatRateLimitMessages();
        this.rateLimitWindowMs = config.chatRateLimitWindowMs();
        this.clock = clock;
    }

    public String clean(String raw) {
        String text = raw.replaceAll("\\p{Cntrl}", " ").trim();
        if (text.isEmpty() || text.length() > maxLength) {
            throw new RequestRejected(ErrorCode.CHAT_INVALID,
                    "Chat messages must be 1 to " + maxLength + " characters");
        }
        return text;
    }

    public void admit(String playerId) {
        long now = clock.getAsLong();
        Deque<Long> times = sendTimes.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() >= rateLimitWindowMs) {
            times.removeFirst();
        }
        if (times.size() >= rateLimitMessages) {
            throw new RequestRejected(ErrorCode.CHAT_RATE_LIMITED, "Slow down: too many messages");
        }
        times.addLast(now);
    }

    public void remember(ChatMessage message) {
        history.addLast(message);
        while (history.size() > historySize) {
            history.removeFirst();
        }
    }

    public List<ChatMessage> recent() {
        return new ArrayList<>(history);
    }

    public long now() {
        return clock.getAsLong();
    }
}
