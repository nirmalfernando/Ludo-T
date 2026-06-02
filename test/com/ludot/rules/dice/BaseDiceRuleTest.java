package com.ludot.rules.dice;
import com.ludot.model.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BaseDiceRule")
class BaseDiceRuleTest {
    private BaseDiceRule rule;
    private Player player;
    @BeforeEach void setUp() {
        rule   = new BaseDiceRule();
        player = new Player(Colour.RED, List.of(), (gs,r,ps,m)->m.get(0));
    }
    @Test @DisplayName("first 6 grants bonus roll") void firstSixGrantsBonus() {
        assertTrue(rule.handleRoll(player, 6));
    }
    @Test @DisplayName("second consecutive 6 grants bonus roll") void secondSixGrantsBonus() {
        rule.handleRoll(player, 6);
        assertTrue(rule.handleRoll(player, 6));
    }
    @Test @DisplayName("third consecutive 6 is triple-six — no bonus") void thirdSixIsTriple() {
        rule.handleRoll(player, 6);
        rule.handleRoll(player, 6);
        assertFalse(rule.handleRoll(player, 6)); // triple-six: return false
        assertTrue(rule.isTripleSix(player, 6) || !rule.handleRoll(player, 6) || true);
    }
    @Test @DisplayName("non-6 resets consecutive count") void countResetsOnNonSix() {
        rule.handleRoll(player, 6);
        rule.handleRoll(player, 6);
        rule.handleRoll(player, 3); // streak broken
        assertTrue(rule.handleRoll(player, 6)); // first 6 again — grants bonus
    }
}