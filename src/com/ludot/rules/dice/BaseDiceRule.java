package com.ludot.rules.dice;

import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.HashMap;
import java.util.Map;

// Template Method — tracks consecutive sixes and handles bonus roll / triple-six logic.
public class BaseDiceRule {

    private final Map<Colour, Integer> consecutiveSixes;

    public BaseDiceRule() {
        this.consecutiveSixes = new HashMap<>();
    }

    // returns true if a bonus roll is granted (rolled 6 but not triple-six)
    public boolean handleRoll(Player player, int roll) {
        if (roll == 6) {
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

    public boolean isTripleSix(Player player, int roll) {
        return consecutiveSixes.getOrDefault(player.getColour(), 0) >= 3;
    }
}
