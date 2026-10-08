package com.ludot.factory;

import com.ludot.dice.FixedRandomProvider;
import com.ludot.rules.RuleEngine;
import com.ludot.rules.capture.BaseCaptureRule;
import com.ludot.rules.capture.LudoTCaptureRule;
import com.ludot.rules.dice.BaseDiceRule;
import com.ludot.rules.dice.LudoTDiceRule;
import com.ludot.rules.home.BaseHomeEntryRule;
import com.ludot.rules.home.LudoTHomeEntryRule;
import com.ludot.rules.move.BaseMoveValidator;
import com.ludot.rules.move.LudoTMoveValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuleEngineFactory")
class RuleEngineFactoryTest {

    @Test @DisplayName("createLudoTRuleEngine returns non-null engine")
    void ludotEngineNotNull() {
        assertNotNull(RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider()));
    }

    @Test @DisplayName("createClassicRuleEngine returns non-null engine")
    void classicEngineNotNull() {
        assertNotNull(RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider()));
    }

    @Test @DisplayName("createLudoTRuleEngine wires LudoT-specific move validator")
    void ludotUsesLudoTMoveValidator() {
        RuleEngine engine = RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider());
        assertInstanceOf(LudoTMoveValidator.class, engine.getMoveValidator());
    }

    @Test @DisplayName("createLudoTRuleEngine wires LudoT capture rule (T-9 reset)")
    void ludotUsesLudoTCaptureRule() {
        RuleEngine engine = RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider());
        assertInstanceOf(LudoTCaptureRule.class, engine.getCaptureRule());
    }

    @Test @DisplayName("createLudoTRuleEngine wires LudoT home-entry rule (T-7 capture gate)")
    void ludotUsesLudoTHomeEntryRule() {
        RuleEngine engine = RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider());
        assertInstanceOf(LudoTHomeEntryRule.class, engine.getHomeEntryRule());
    }

    @Test @DisplayName("createLudoTRuleEngine wires LudoT dice rule (T-6 block break)")
    void ludotUsesLudoTDiceRule() {
        RuleEngine engine = RuleEngineFactory.createLudoTRuleEngine(new FixedRandomProvider());
        assertInstanceOf(LudoTDiceRule.class, engine.getDiceRule());
    }

    @Test @DisplayName("createClassicRuleEngine uses base (non-LudoT) move validator")
    void classicUsesBaseMoveValidator() {
        RuleEngine engine = RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider());
        assertInstanceOf(BaseMoveValidator.class, engine.getMoveValidator());
        assertFalse(engine.getMoveValidator() instanceof LudoTMoveValidator,
                "Classic engine must not use LudoT move validator");
    }

    @Test @DisplayName("createClassicRuleEngine uses base capture rule (no T-9 reset)")
    void classicUsesBaseCaptureRule() {
        RuleEngine engine = RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider());
        assertInstanceOf(BaseCaptureRule.class, engine.getCaptureRule());
        assertFalse(engine.getCaptureRule() instanceof LudoTCaptureRule,
                "Classic engine must not use LudoT capture rule");
    }

    @Test @DisplayName("createClassicRuleEngine uses base home-entry rule (no capture gate)")
    void classicUsesBaseHomeEntryRule() {
        RuleEngine engine = RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider());
        assertInstanceOf(BaseHomeEntryRule.class, engine.getHomeEntryRule());
        assertFalse(engine.getHomeEntryRule() instanceof LudoTHomeEntryRule,
                "Classic engine must not use LudoT home-entry rule");
    }

    @Test @DisplayName("createClassicRuleEngine uses base dice rule (no block-break)")
    void classicUsesBaseDiceRule() {
        RuleEngine engine = RuleEngineFactory.createClassicRuleEngine(new FixedRandomProvider());
        assertInstanceOf(BaseDiceRule.class, engine.getDiceRule());
        assertFalse(engine.getDiceRule() instanceof LudoTDiceRule,
                "Classic engine must not use LudoT dice rule");
    }
}
