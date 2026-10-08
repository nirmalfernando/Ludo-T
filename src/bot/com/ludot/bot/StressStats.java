package com.ludot.bot;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

final class StressStats {

    private static final int MAX_SAMPLES = 10;

    private final ConcurrentHashMap<String, LongAdder> outcomes = new ConcurrentHashMap<>();
    private final LongAdder unexpected = new LongAdder();
    private final LongAdder timeouts = new LongAdder();
    private final LongAdder responses = new LongAdder();
    private final LongAdder duplicatesReplayed = new LongAdder();
    private final LongAdder chatSent = new LongAdder();
    private final LongAdder chatAccepted = new LongAdder();
    private final LongAdder chatRateLimited = new LongAdder();
    private final ConcurrentLinkedQueue<String> unexpectedSamples = new ConcurrentLinkedQueue<>();
    private final AtomicLongArray latenciesNanos;

    StressStats(int expectedRequests) {
        this.latenciesNanos = new AtomicLongArray(expectedRequests);
    }

    void outcome(String label) {
        outcomes.computeIfAbsent(label, key -> new LongAdder()).increment();
    }

    void response() {
        responses.increment();
    }

    void unexpected(String description) {
        unexpected.increment();
        if (unexpectedSamples.size() < MAX_SAMPLES) {
            unexpectedSamples.add(description);
        }
    }

    void timeout() {
        timeouts.increment();
    }

    void duplicateReplayed() {
        duplicatesReplayed.increment();
    }

    void chat(boolean accepted) {
        chatSent.increment();
        (accepted ? chatAccepted : chatRateLimited).increment();
    }

    void latency(int index, long nanos) {
        if (index < latenciesNanos.length()) {
            latenciesNanos.set(index, Math.max(1, nanos));
        }
    }

    long responseCount() {
        return responses.sum();
    }

    long unexpectedCount() {
        return unexpected.sum();
    }

    long timeoutCount() {
        return timeouts.sum();
    }

    long duplicatesReplayedCount() {
        return duplicatesReplayed.sum();
    }

    long chatSentCount() {
        return chatSent.sum();
    }

    long chatAcceptedCount() {
        return chatAccepted.sum();
    }

    long chatRateLimitedCount() {
        return chatRateLimited.sum();
    }

    Map<String, Long> outcomeCounts() {
        Map<String, Long> counts = new TreeMap<>();
        outcomes.forEach((label, adder) -> counts.put(label, adder.sum()));
        return counts;
    }

    java.util.List<String> unexpectedSamples() {
        return java.util.List.copyOf(unexpectedSamples);
    }

    long percentileMs(int percentile) {
        long[] recorded = recorded();
        if (recorded.length == 0) {
            return 0;
        }
        int index = (int) Math.min(recorded.length - 1, Math.ceil(percentile / 100.0 * recorded.length) - 1);
        return recorded[Math.max(0, index)] / 1_000_000;
    }

    long maxMs() {
        long[] recorded = recorded();
        return recorded.length == 0 ? 0 : recorded[recorded.length - 1] / 1_000_000;
    }

    private long[] recorded() {
        long[] all = new long[latenciesNanos.length()];
        int count = 0;
        for (int i = 0; i < all.length; i++) {
            long value = latenciesNanos.get(i);
            if (value > 0) {
                all[count++] = value;
            }
        }
        long[] trimmed = Arrays.copyOf(all, count);
        Arrays.sort(trimmed);
        return trimmed;
    }
}
