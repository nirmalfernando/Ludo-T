package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.rules.RuleEngine;
import com.ludot.rules.capture.BaseCaptureRule;
import com.ludot.rules.dice.BaseDiceRule;
import com.ludot.rules.home.BaseHomeEntryRule;
import com.ludot.rules.move.BaseMoveValidator;

// Concrete Abstract Factory — wires the classic Ludo rule set (Rules 1-11, always CW, no T-extensions).
public class ClassicRuleEngineFactory extends RuleEngineFactory {

    @Override
    public RuleEngine create(RandomProvider random) {
        return new RuleEngine(
                new BaseMoveValidator(random) {}, // anonymous subclass — uses base CW-only defaults
                new BaseCaptureRule(),
                null, // Classic Ludo has no block rule (Rule 7 prohibits self-landing; T-3 not active)
                new BaseHomeEntryRule(),
                new BaseDiceRule(),
                null, // no mystery cell in Classic Ludo
                null  // no effects in Classic Ludo
        );
    }
}
