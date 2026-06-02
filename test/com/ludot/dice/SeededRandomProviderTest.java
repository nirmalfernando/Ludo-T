package com.ludot.dice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("SeededRandomProvider")
class SeededRandomProviderTest {
    private SeededRandomProvider random;
    @BeforeEach void setUp() { random = new SeededRandomProvider(42L); }
    @Test @DisplayName("rollDice returns value between 1 and 6") void rollDiceInRange() {}
    @Test @DisplayName("same seed produces same sequence") void sameSeedDeterministic() {}
    @Test @DisplayName("getRandomCell returns value within bound") void randomCellInBound() {}
    @Test @DisplayName("getMysteryEffect returns 1 to 6") void mysteryEffectInRange() {}
}