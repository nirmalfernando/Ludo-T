package com.ludot.rules.dice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("BaseDiceRule")
class BaseDiceRuleTest {
    @Test @DisplayName("first 6 grants bonus roll") void firstSixGrantsBonus() {}
    @Test @DisplayName("second 6 grants bonus roll") void secondSixGrantsBonus() {}
    @Test @DisplayName("third 6 is triple-six — no bonus") void thirdSixIsTriple() {}
    @Test @DisplayName("non-6 resets consecutive count") void countResetsOnNonSix() {}
}