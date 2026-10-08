package com.ludot.rules.dice;

import com.ludot.GameConstants;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.HashMap;
import java.util.Map;

public class BaseDiceRule {

    private final Map<Colour, Integer> consecutiveSixes;

    
    public BaseDiceRule() {
        this.consecutiveSixes = new HashMap<>();
    }

    
    // consecutive sixes count within one turn (Rule 4): a new turn starts from zero
    public final void startTurn(Player player) {
        consecutiveSixes.put(player.getColour(), 0);
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

}
