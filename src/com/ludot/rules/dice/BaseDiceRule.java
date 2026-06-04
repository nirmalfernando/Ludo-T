package com.ludot.rules.dice;

import com.ludot.GameConstants;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import com.ludot.rules.GameRule;
import com.ludot.rules.MoveContext;
import com.ludot.rules.RuleResult;
import java.util.HashMap;
import java.util.Map;

public class BaseDiceRule implements GameRule {

    private final Map<Colour, Integer> consecutiveSixes;

    
    public BaseDiceRule() {
        this.consecutiveSixes = new HashMap<>();
    }

    
    public final boolean handleRoll(Player player, int roll) {
        if (roll == GameConstants.LEAVE_BASE_ROLL) {
            int count = consecutiveSixes.getOrDefault(player.getColour(), 0) + 1;
            consecutiveSixes.put(player.getColour(), count);
            if (isTripleSix(player, roll)) {
                consecutiveSixes.put(player.getColour(), 0);
                return false; // triple-six: penalise, no bonus
            }
            return true; // bonus roll
        }
        consecutiveSixes.put(player.getColour(), 0); // streak broken
        return false;
    }

    
    protected boolean isTripleSix(Player player, int roll) {
        return consecutiveSixes.getOrDefault(player.getColour(), 0) >= GameConstants.CONSECUTIVE_SIXES_LIMIT;
    }

    @Override
    public boolean isApplicable(MoveContext ctx) {
        return ctx.player() != null && ctx.roll() > 0;
    }

    @Override
    public RuleResult apply(MoveContext ctx) {
        boolean grantsBonus = ctx.roll() == GameConstants.LEAVE_BASE_ROLL;
        return grantsBonus ? RuleResult.success("Roll of 6 may grant a bonus roll")
                           : RuleResult.success("Normal roll — no bonus");
    }

    @Override
    public int getPriority() { return 20; }
}
