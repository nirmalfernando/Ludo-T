package com.ludot.rules.dice;
import com.ludot.rules.block.BlockRule;

// T-6: triple-six with an active block forces a block break — handled by TurnManager after detection.
// Holds the BlockRule reference so TurnManager can access it via RuleEngine.getDiceRule().
public class LudoTDiceRule extends BaseDiceRule {

    private final BlockRule blockRule;

    public LudoTDiceRule(BlockRule blockRule) {
        super();
        this.blockRule = blockRule;
    }

    public BlockRule getBlockRule() { return blockRule; }
}
