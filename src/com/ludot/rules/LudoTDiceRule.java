package com.ludot.rules;

import com.ludot.model.Player;

// LUDO-T specific dice rule that integrates block-breaking on the triple-six penalty (T-6).

public class LudoTDiceRule extends BaseDiceRule {

    private final BlockRule blockRule;

    public LudoTDiceRule(BlockRule blockRule) {
        super();
        this.blockRule = blockRule;
    }

    @Override
    public boolean isTripleSix(Player player, int roll) {
        return false;
    }
}
