package com.ludot.factory;

import com.ludot.dice.RandomProvider;
import com.ludot.rules.RuleEngine;
import com.ludot.rules.block.BlockRule;
import com.ludot.rules.capture.LudoTCaptureRule;
import com.ludot.rules.dice.LudoTDiceRule;
import com.ludot.rules.home.LudoTHomeEntryRule;
import com.ludot.rules.move.LudoTMoveValidator;
import com.ludot.rules.mystery.EffectRule;
import com.ludot.rules.mystery.MysteryRule;

// Concrete Abstract Factory — wires the full LUDO-T rule set (T-1 through T-15 on top of classic rules).
public class LudoTRuleEngineFactory extends RuleEngineFactory {

    @Override
    public RuleEngine create(RandomProvider random) {
        BlockRule blockRule = new BlockRule();
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
