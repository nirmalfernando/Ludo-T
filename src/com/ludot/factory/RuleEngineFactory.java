package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.rules.BaseCaptureRule;
import com.ludot.rules.BaseDiceRule;
import com.ludot.rules.BaseHomeEntryRule;
import com.ludot.rules.BaseMoveValidator;
import com.ludot.rules.BlockRule;
import com.ludot.rules.EffectRule;
import com.ludot.rules.LudoTCaptureRule;
import com.ludot.rules.LudoTDiceRule;
import com.ludot.rules.LudoTHomeEntryRule;
import com.ludot.rules.LudoTMoveValidator;
import com.ludot.rules.MysteryRule;
import com.ludot.rules.RuleEngine;

// Abstract Factory — assembles different RuleEngine configurations (classic vs LudoT variant).
public class RuleEngineFactory {

    private RuleEngineFactory() {}

    // base Ludo: always CW, no capture bonus, no approach-count requirement
    public static RuleEngine createClassicRuleEngine(RandomProvider random) {
        BlockRule blockRule = new BlockRule(random);
        return new RuleEngine(
                new BaseMoveValidator(random) {}, // anonymous — uses base CW-only defaults
                new BaseCaptureRule(),
                blockRule,
                new BaseHomeEntryRule(),
                new BaseDiceRule(),
                new MysteryRule(random),
                new EffectRule(random)
        );
    }

    // LudoT: coin-toss direction, T-7 capture requirement, T-2 bonus roll, T-6 block break
    public static RuleEngine createLudoTRuleEngine(RandomProvider random) {
        BlockRule blockRule = new BlockRule(random);
        LudoTDiceRule diceRule = new LudoTDiceRule(blockRule);
        return new RuleEngine(
                new LudoTMoveValidator(random),
                new LudoTCaptureRule(),
                blockRule,
                new LudoTHomeEntryRule(),
                diceRule,
                new MysteryRule(random),
                new EffectRule(random)
        );
    }
}
