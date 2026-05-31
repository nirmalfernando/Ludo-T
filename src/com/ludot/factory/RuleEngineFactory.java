package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.rules.RuleEngine;

// Abstract Factory that assembles different RuleEngine configurations (classic vs LUDO-T variant).

public class RuleEngineFactory {

    private RuleEngineFactory() {
    }

    public static RuleEngine createClassicRuleEngine(RandomProvider random) {
        return null;
    }

    public static RuleEngine createLudoTRuleEngine(RandomProvider random) {
        return null;
    }
}
