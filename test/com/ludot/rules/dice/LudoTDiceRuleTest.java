package com.ludot.rules.dice;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import com.ludot.rules.block.BlockRule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("LudoTDiceRule (T-6)")
class LudoTDiceRuleTest {
    @Test @DisplayName("getBlockRule returns injected BlockRule") void exposesBlockRule() {
        BlockRule br = new BlockRule(new FixedRandomProvider());
        LudoTDiceRule rule = new LudoTDiceRule(br);
        assertSame(br, rule.getBlockRule());
    }
    @Test @DisplayName("isTripleSix delegates to parent count tracking") void delegatesToParent() {
        LudoTDiceRule rule = new LudoTDiceRule(new BlockRule(new FixedRandomProvider()));
        Player player = new Player(Colour.RED, List.of(), (gs,r,ps,m)->m.get(0));
        rule.handleRoll(player, 6);
        rule.handleRoll(player, 6);
        assertFalse(rule.handleRoll(player, 6)); // 3rd six = triple, returns false
    }
}