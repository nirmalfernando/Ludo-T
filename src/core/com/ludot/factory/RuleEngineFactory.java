package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.rules.RuleEngine;

public abstract class RuleEngineFactory {

    // Produce a fully wired RuleEngine for a specific game variant.
    public abstract RuleEngine create(RandomProvider random);

    public static RuleEngine createClassicRuleEngine(RandomProvider random) {
        return new ClassicRuleEngineFactory().create(random);
    }

    // Convenience static accessor — delegates to LudoTRuleEngineFactory.
    public static RuleEngine createLudoTRuleEngine(RandomProvider random) {
        return new LudoTRuleEngineFactory().create(random);
    }
}
