package com.ludot.bot;

import java.util.Map;
import java.util.Locale;

/** What a stress run measured; {@link #toString()} prints it in the format the test plan specifies. */
public record StressReport(String name, int sent, long responses, long timeouts, double durationSeconds,
                           Map<String, Long> outcomes, long unexpected, java.util.List<String> unexpectedSamples,
                           long p50Ms, long p95Ms, long maxMs, long chatSent, long chatAccepted,
                           long chatRateLimited, long chatOrderHash, long chatLinesSeen, long updatesSeen,
                           long hashMismatches, String finalHash) {

    /** True when every response was expected, nothing timed out and no state update failed its own hash check. */
    public boolean clean() {
        return unexpected == 0 && timeouts == 0 && hashMismatches == 0 && !finalHash.contains("MISMATCH");
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("=== ").append(name).append(" ===\n");
        out.append(String.format(Locale.ROOT, "sent=%d  responses=%d  timeouts=%d  duration=%.2fs  throughput=%.0f req/s%n",
                sent, responses, timeouts, durationSeconds, durationSeconds > 0 ? sent / durationSeconds : 0));
        outcomes.forEach((label, count) -> out.append(label).append('=').append(count).append("  "));
        out.append('\n');
        out.append("UNEXPECTED=").append(unexpected).append('\n');
        unexpectedSamples.forEach(sample -> out.append("  unexpected: ").append(sample).append('\n'));
        out.append(String.format(Locale.ROOT, "latency ms: p50=%d  p95=%d  max=%d%n", p50Ms, p95Ms, maxMs));
        out.append(String.format(Locale.ROOT, "chat: sent=%d accepted=%d rateLimited=%d  chatOrderHash=%08X (%d lines seen)%n",
                chatSent, chatAccepted, chatRateLimited, chatOrderHash, chatLinesSeen));
        out.append(String.format(Locale.ROOT, "updates seen=%d  hashMismatches=%d%n", updatesSeen, hashMismatches));
        out.append("final stateHash ").append(finalHash).append('\n');
        return out.toString();
    }
}
