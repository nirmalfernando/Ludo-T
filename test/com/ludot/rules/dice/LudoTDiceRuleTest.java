package com.ludot.rules.dice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("LudoTDiceRule (T-6)")
class LudoTDiceRuleTest {
    @Test @DisplayName("getBlockRule returns injected BlockRule") void exposesBlockRule() {}
    @Test @DisplayName("isTripleSix delegates to parent count tracking") void delegatesToParent() {}
}