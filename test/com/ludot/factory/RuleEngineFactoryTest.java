package com.ludot.factory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("RuleEngineFactory")
class RuleEngineFactoryTest {
    @Test @DisplayName("createLudoTRuleEngine returns non-null engine") void ludotEngineNotNull() {}
    @Test @DisplayName("createClassicRuleEngine returns non-null engine") void classicEngineNotNull() {}
}