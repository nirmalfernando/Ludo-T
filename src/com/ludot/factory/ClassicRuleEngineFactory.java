package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.rules.RuleEngine;
import com.ludot.rules.block.BlockRule;
import com.ludot.rules.capture.BaseCaptureRule;
import com.ludot.rules.dice.BaseDiceRule;
import com.ludot.rules.home.BaseHomeEntryRule;
import com.ludot.rules.move.BaseMoveValidator;
import com.ludot.rules.mystery.EffectRule;
import com.ludot.rules.mystery.MysteryRule;

// Concrete Abstract Factory — wires the classic Ludo rule set (Rules 1-11, always CW, no T-extensions).
public class ClassicRuleEngineFactory extends RuleEngineFactory {

    @Override
    public RuleEngine create(RandomProvider random) {
        BlockRule blockRule = new BlockRule(random);
        return new RuleEngine(
                new BaseMoveValidator(random) {}, // anonymous subclass — uses base CW-only defaults
                new BaseCaptureRule(),
                blockRule,
                new BaseHomeEntryRule(),
                new BaseDiceRule(),
                new MysteryRule(random),
                new EffectRule(random)
        );
    }
}
