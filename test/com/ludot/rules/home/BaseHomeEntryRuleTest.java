package com.ludot.rules.home;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("BaseHomeEntryRule")
class BaseHomeEntryRuleTest {
    @Test @DisplayName("canEnterHomeStraight true at approach cell") void trueAtApproach() {}
    @Test @DisplayName("canEnterHomeStraight false elsewhere") void falseElsewhere() {}
}