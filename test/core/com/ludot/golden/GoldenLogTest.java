package com.ludot.golden;

import com.ludot.Main;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression safety net for the Assignment 2 refactor: a full local game for each documented seed
 * must produce a log byte-identical to the one captured from the Assignment 1 code.
 */
@DisplayName("Golden game logs")
class GoldenLogTest {

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 2069})
    void fullGameLogMatchesGolden(long seed) throws IOException {
        String expected = Files.readString(Path.of("test-resources", "golden", "seed-" + seed + ".log"),
                StandardCharsets.UTF_8);
        assertEquals(expected, runGame(seed));
    }

    private static String runGame(long seed) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Main.main(new String[]{Long.toString(seed)});
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
