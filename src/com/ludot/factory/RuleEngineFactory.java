package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.rules.RuleEngine;

// Abstract Factory — subclasses assemble a complete, consistent family of rule objects.
// Use createClassicRuleEngine() / createLudoTRuleEngine() for the two standard configurations,
// or subclass RuleEngineFactory and pass an instance to GameBuilder.withRuleEngineFactory().
public abstract class RuleEngineFactory {

    // Produce a fully wired RuleEngine for a specific game variant.
    public abstract RuleEngine create(RandomProvider random);

    // Convenience static accessor — delegates to ClassicRuleEngineFactory so existing callers
    // (tests, legacy code) continue to work without modification.
    public static RuleEngine createClassicRuleEngine(RandomProvider random) {
        return new ClassicRuleEngineFactory().create(random);
    }

    // Convenience static accessor — delegates to LudoTRuleEngineFactory.
    public static RuleEngine createLudoTRuleEngine(RandomProvider random) {
        return new LudoTRuleEngineFactory().create(random);
    }
}
