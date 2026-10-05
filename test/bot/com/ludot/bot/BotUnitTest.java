package com.ludot.bot;

import com.ludot.protocol.Colour;
import com.ludot.protocol.ErrorCode;
import com.ludot.protocol.Location;
import com.ludot.protocol.MoveKind;
import com.ludot.protocol.MoveOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bot helpers")
class BotUnitTest {

    private static MoveOption option(int index, MoveKind kind, boolean captures, boolean reachesHome,
                                     boolean formsBlock, Location to) {
        return new MoveOption(index, kind, 0, -1, Location.TRACK, 10, to, 12, captures, captures ? 1 : 0,
                formsBlock, false, false, false, reachesHome, "option " + index);
    }

    private static MoveOption plain(int index) {
        return option(index, MoveKind.MOVE, false, false, false, Location.TRACK);
    }

    private static MoveOption skip(int index) {
        return option(index, MoveKind.SKIP, false, false, false, Location.BASE);
    }

    // ---------------------------------------------------------------- args

    @Test @DisplayName("options parse as --key value pairs and flags")
    void argsParse() {
        Args args = Args.parse(new String[]{"--host", "10.0.0.5", "--port", "6000", "--create", "--delay", "250"});
        assertEquals("10.0.0.5", args.get("host", "x"));
        assertEquals(6000, args.getInt("port", 1));
        assertTrue(args.has("create"));
        assertEquals(250, args.getLong("delay", 0));
        assertEquals("fallback", args.get("missing", "fallback"));
    }

    @Test @DisplayName("a missing required option is reported by name")
    void argsRequire() {
        Args args = Args.parse(new String[]{"--port", "1"});
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> args.require("room"));
        assertTrue(e.getMessage().contains("--room"));
    }

    @Test @DisplayName("a stray value without a key is rejected")
    void argsStray() {
        assertThrows(IllegalArgumentException.class, () -> Args.parse(new String[]{"oops"}));
    }

    // ---------------------------------------------------------------- strategies

    @Test @DisplayName("RED prefers a capture over every other move")
    void redCaptures() {
        List<MoveOption> options = List.of(plain(0), option(1, MoveKind.CAPTURE, true, false, false, Location.TRACK),
                option(2, MoveKind.REACH_HOME, false, true, false, Location.HOME));
        assertEquals(1, BotStrategies.forColour(Colour.RED, new Random(1)).choose(options));
    }

    @Test @DisplayName("YELLOW prefers reaching home over a capture")
    void yellowRaces() {
        List<MoveOption> options = List.of(option(0, MoveKind.CAPTURE, true, false, false, Location.TRACK),
                option(1, MoveKind.REACH_HOME, false, true, false, Location.HOME));
        assertEquals(1, BotStrategies.forColour(Colour.YELLOW, new Random(1)).choose(options));
    }

    @Test @DisplayName("GREEN prefers forming a blockade")
    void greenBlocks() {
        List<MoveOption> options = List.of(plain(0), option(1, MoveKind.MOVE, false, false, true, Location.TRACK));
        assertEquals(1, BotStrategies.forColour(Colour.GREEN, new Random(1)).choose(options));
    }

    @Test @DisplayName("every colour takes a real move over a skip, and the skip when it is all there is")
    void skipIsLastResort() {
        for (Colour colour : Colour.values()) {
            BotStrategy strategy = BotStrategies.forColour(colour, new Random(3));
            assertEquals(0, strategy.choose(List.of(skip(0))));
            if (colour != Colour.BLUE) {
                assertEquals(1, strategy.choose(List.of(skip(0), plain(1))));
            }
        }
    }

    @Test @DisplayName("BLUE chooses at random but the same seed gives the same choices, always in range")
    void blueIsSeededRandom() {
        List<MoveOption> options = List.of(plain(0), plain(1), plain(2), plain(3));
        BotStrategy a = BotStrategies.forColour(Colour.BLUE, new Random(42));
        BotStrategy b = BotStrategies.forColour(Colour.BLUE, new Random(42));
        Map<Integer, Integer> seen = new java.util.HashMap<>();
        for (int i = 0; i < 200; i++) {
            int choice = a.choose(options);
            assertEquals(choice, b.choose(options));
            assertTrue(choice >= 0 && choice < options.size());
            seen.merge(choice, 1, Integer::sum);
        }
        assertEquals(4, seen.size(), "all options should come up eventually");
    }

    // ---------------------------------------------------------------- request mix

    @Test @DisplayName("the random mix never picks LEGIT and follows the weights")
    void mixFollowsWeights() {
        Map<RequestKind, Integer> counts = new EnumMap<>(RequestKind.class);
        int samples = 100_000;
        Random random = new Random(7);
        for (int i = 0; i < samples; i++) {
            counts.merge(RequestKind.pick(random.nextDouble()), 1, Integer::sum);
        }
        assertNull(counts.get(RequestKind.LEGIT));
        // weights total 70: STALE_MOVE 15 of 70 is about 21%
        assertEquals(15 / 70.0, counts.get(RequestKind.STALE_MOVE) / (double) samples, 0.01);
        assertEquals(5 / 70.0, counts.get(RequestKind.CHAT) / (double) samples, 0.01);
    }

    @Test @DisplayName("each kind accepts its documented outcomes and rejects the others")
    void kindsJudgeOutcomes() {
        assertTrue(RequestKind.STALE_MOVE.accepts(false, ErrorCode.STALE_TURN));
        assertTrue(RequestKind.STALE_MOVE.accepts(false, ErrorCode.GAME_FINISHED));
        assertFalse(RequestKind.STALE_MOVE.accepts(true, null), "a stale move must never succeed");
        assertFalse(RequestKind.STALE_MOVE.accepts(false, ErrorCode.INTERNAL_ERROR));
        assertTrue(RequestKind.CHAT.accepts(true, null));
        assertTrue(RequestKind.CHAT.accepts(false, ErrorCode.CHAT_RATE_LIMITED));
        assertFalse(RequestKind.CHAT.accepts(false, ErrorCode.CHAT_INVALID));
        assertTrue(RequestKind.PAUSE_NOT_HOST.accepts(false, ErrorCode.NOT_HOST));
        assertFalse(RequestKind.PAUSE_NOT_HOST.accepts(true, null));
        assertTrue(RequestKind.BAD_INDEX.accepts(false, ErrorCode.INVALID_MOVE));
        assertFalse(RequestKind.SNAPSHOT.accepts(false, ErrorCode.NOT_IN_ROOM));
    }

    // ---------------------------------------------------------------- stats

    @Test @DisplayName("percentiles come from the recorded latencies and ignore unanswered slots")
    void percentiles() {
        StressStats stats = new StressStats(100);
        for (int i = 0; i < 90; i++) {
            stats.latency(i, (i + 1) * 1_000_000L); // 1..90 ms; slots 90-99 stay unanswered
        }
        assertEquals(45, stats.percentileMs(50));
        assertEquals(86, stats.percentileMs(95));
        assertEquals(90, stats.maxMs());
    }

    @Test @DisplayName("an empty run reports zero latency rather than failing")
    void emptyStats() {
        StressStats stats = new StressStats(10);
        assertEquals(0, stats.percentileMs(95));
        assertEquals(0, stats.maxMs());
    }

    @Test @DisplayName("counters stay exact when many threads increment at once")
    void countersAreExactUnderContention() throws Exception {
        StressStats stats = new StressStats(1);
        Thread[] threads = new Thread[16];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 10_000; i++) {
                    stats.outcome("OK");
                    stats.response();
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(160_000, stats.responseCount());
        assertEquals(160_000L, stats.outcomeCounts().get("OK"));
    }

    @Test @DisplayName("the report prints the fields the test plan lists and says whether the run was clean")
    void reportFormat() {
        StressReport report = new StressReport("stressA", 2000, 2000, 0, 4.0, new java.util.TreeMap<>(Map.of("OK", 612L)),
                0, List.of(), 4, 19, 88, 100, 25, 75, 0x41C0D2E7L, 25, 900, 0, "client=9F21A3C0 server=9F21A3C0  MATCH");
        String text = report.toString();
        assertTrue(text.contains("=== stressA ==="));
        assertTrue(text.contains("throughput=500 req/s"));
        assertTrue(text.contains("UNEXPECTED=0"));
        assertTrue(text.contains("p50=4  p95=19  max=88"));
        assertTrue(text.contains("chatOrderHash=41C0D2E7"));
        assertTrue(report.clean());
        StressReport bad = new StressReport("x", 1, 1, 0, 1.0, Map.of(), 1, List.of("boom"), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "n/a");
        assertFalse(bad.clean());
    }
}
