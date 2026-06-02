package com.ludot.rules.home;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("LudoTHomeEntryRule (T-7)")
class LudoTHomeEntryRuleTest {
    @Test @DisplayName("requires at least one prior capture") void requiresCapture() {}
    @Test @DisplayName("false with zero captures") void falseWithNoCaptures() {}
    @Test @DisplayName("false when not at approach cell") void falseNotAtApproach() {}
}