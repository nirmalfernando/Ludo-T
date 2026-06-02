package com.ludot.factory;
import com.ludot.dice.FixedRandomProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("RuleEngineFactory")
class RuleEngineFactoryTest {
    @Test @DisplayName("createLudoTRuleEngine returns non-null engine") void ludotEngineNotNull() {
        assertNotNull(RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider()));
    }
    @Test @DisplayName("createClassicRuleEngine returns non-null engine") void classicEngineNotNull() {
        assertNotNull(RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider()));
    }
}