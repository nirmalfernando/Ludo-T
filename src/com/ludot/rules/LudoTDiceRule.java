package com.ludot.rules;

import com.ludot.model.Player;

// T-6: triple-six with an active block forces a block break — handled by TurnManager after detection.
public class LudoTDiceRule extends BaseDiceRule {

    private final BlockRule blockRule;

    public LudoTDiceRule(BlockRule blockRule) {
        super();
        this.blockRule = blockRule;
    }

    @Override
    public boolean isTripleSix(Player player, int roll) {
        return super.isTripleSix(player, roll);
    }

    public BlockRule getBlockRule() { return blockRule; }
}
