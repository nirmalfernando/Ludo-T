package com.ludot.rules;

import java.util.Map;
import com.ludot.model.Colour;
import com.ludot.model.Player;

// Template Method base for dice-roll handling including bonus-roll and triple-six penalty logic.

public class BaseDiceRule {

    protected Map<Colour, Integer> consecutiveSixes;

    public BaseDiceRule() {
    }

    public boolean handleRoll(Player player, int roll) {
        return false;
    }

    public boolean isTripleSix(Player player, int roll) {
        return false;
    }
}
