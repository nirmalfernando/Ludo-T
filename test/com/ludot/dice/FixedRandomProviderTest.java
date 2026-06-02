package com.ludot.dice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("FixedRandomProvider")
class FixedRandomProviderTest {
    @Test @DisplayName("returns queued dice values in order") void returnsDiceInOrder() {
        FixedRandomProvider fp = new FixedRandomProvider().addDice(3, 6, 1);
        assertEquals(3, fp.rollDice());
        assertEquals(6, fp.rollDice());
        assertEquals(1, fp.rollDice());
    }
    @Test @DisplayName("defaults to 1 when dice queue empty") void defaultsWhenEmpty() {
        assertEquals(1, new FixedRandomProvider().rollDice());
    }
    @Test @DisplayName("getMysteryEffect defaults to 1") void mysteryEffectDefault() {
        assertEquals(1, new FixedRandomProvider().getMysteryEffect());
    }
}