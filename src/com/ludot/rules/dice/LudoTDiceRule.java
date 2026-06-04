package com.ludot.rules.dice;

import com.ludot.rules.block.BlockRule;

public class LudoTDiceRule extends BaseDiceRule {

    private final BlockRule blockRule;

    
    public LudoTDiceRule(BlockRule blockRule) {
        super();
        this.blockRule = blockRule;
    }

    
    public BlockRule getBlockRule() {
        return blockRule;
    }
}
