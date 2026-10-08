package com.ludot.dice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("SeededRandomProvider")
class SeededRandomProviderTest {
    private SeededRandomProvider random;
    @BeforeEach void setUp() { random = new SeededRandomProvider(42L); }
    @Test @DisplayName("rollDice returns value between 1 and 6") void rollDiceInRange() {
        for (int i = 0; i < 60; i++) {
            int r = new SeededRandomProvider(i).rollDice();
            assertTrue(r >= 1 && r <= 6, "expected 1-6, got " + r);
        }
    }
    @Test @DisplayName("same seed produces same sequence") void sameSeedDeterministic() {
        SeededRandomProvider r1 = new SeededRandomProvider(42L);
        SeededRandomProvider r2 = new SeededRandomProvider(42L);
        for (int i = 0; i < 10; i++) assertEquals(r1.rollDice(), r2.rollDice());
    }
    @Test @DisplayName("getRandomCell returns value within bound") void randomCellInBound() {
        for (int i = 0; i < 30; i++) {
            int c = random.getRandomCell(52);
            assertTrue(c >= 0 && c < 52);
        }
    }
    @Test @DisplayName("getMysteryEffect returns 1 to 6") void mysteryEffectInRange() {
        for (int i = 0; i < 30; i++) {
            int m = random.getMysteryEffect();
            assertTrue(m >= 1 && m <= 6);
        }
    }
}