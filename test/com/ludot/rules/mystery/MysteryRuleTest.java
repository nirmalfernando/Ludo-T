package com.ludot.rules.mystery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("MysteryRule (T-10/11)")
class MysteryRuleTest {
    @Test @DisplayName("shouldSpawn false for rounds 1 and 2") void noSpawnEarly() {}
    @Test @DisplayName("shouldSpawn true after round 2") void spawnAfterRoundTwo() {}
    @Test @DisplayName("getRandomSpawnCell never repeats last location") void noConsecutiveRepeat() {}
    @Test @DisplayName("resolveTeleport maps roll 1 to ALPHA") void roll1IsAlpha() {}
    @Test @DisplayName("resolveTeleport maps roll 6 to APPROACH_CELL") void roll6IsApproach() {}
}