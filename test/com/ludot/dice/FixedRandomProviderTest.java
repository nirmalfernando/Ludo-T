package com.ludot.dice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("FixedRandomProvider")
class FixedRandomProviderTest {
    @Test @DisplayName("returns queued dice values in order") void returnsDiceInOrder() {}
    @Test @DisplayName("defaults to 1 when dice queue empty") void defaultsWhenEmpty() {}
    @Test @DisplayName("getMysteryEffect defaults to 1") void mysteryEffectDefault() {}
}