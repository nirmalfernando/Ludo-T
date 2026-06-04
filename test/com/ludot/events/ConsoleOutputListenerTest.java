package com.ludot.events;

import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.events.mystery.MysteryExpireEvent;
import com.ludot.events.mystery.MysterySpawnEvent;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConsoleOutputListener — Visitor routing and output")
class ConsoleOutputListenerTest {

    private ConsoleOutputListener listener;
    private EventBus bus;
    private Player player;
    private PrintStream originalOut;
    private ByteArrayOutputStream captured;

    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        listener = new ConsoleOutputListener(new OutputFormatter());
        bus = new EventBus();
        bus.subscribe(listener);
        player = new Player(Colour.RED, List.of(), (gs, r, ps, m) -> m.get(0));
        originalOut = System.out;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
    }

    @AfterEach void tearDown() {
        System.setOut(originalOut);
        Board.resetInstance();
    }

    private String output() { return captured.toString(); }

    // ── routing — each visit method reaches the formatter ────────────────────

    @Test @DisplayName("DiceRollEvent produces non-blank output containing roll value")
    void diceRollRoutedCorrectly() {
        bus.publish(new DiceRollEvent(player, 5));
        assertFalse(output().isBlank(), "DiceRollEvent must produce output");
        assertTrue(output().contains("5"), "Output must contain the roll value");
    }

    @Test @DisplayName("RollOffEvent produces output with player colour and roll")
    void rollOffRoutedCorrectly() {
        bus.publish(new RollOffEvent(player, 4));
        assertTrue(output().contains("4"), "Output must contain roll value");
    }

    @Test @DisplayName("SkipTurnEvent produces output mentioning the player")
    void skipTurnRoutedCorrectly() {
        bus.publish(new SkipTurnEvent(player));
        assertFalse(output().isBlank(), "SkipTurnEvent must produce output");
        assertTrue(output().toLowerCase().contains("red"),
                "Output must reference the skipped player's colour");
    }

    @Test @DisplayName("WinEvent produces output with winner's colour")
    void winEventRoutedCorrectly() {
        bus.publish(new WinEvent(player, 42));
        assertTrue(output().toLowerCase().contains("red"),
                "WinEvent output must include winner's colour");
        assertTrue(output().contains("42"),
                "WinEvent output must include round count");
    }

    @Test @DisplayName("MysterySpawnEvent produces output with cell location")
    void mysterySpawnRoutedCorrectly() {
        Cell cell = Board.getInstance().getCell(7);
        bus.publish(new MysterySpawnEvent(cell, 4));
        assertTrue(output().contains("7"),
                "MysterySpawnEvent output must include cell ID");
    }

    @Test @DisplayName("MysteryExpireEvent produces non-blank output")
    void mysteryExpireRoutedCorrectly() {
        Cell cell = Board.getInstance().getCell(7);
        bus.publish(new MysteryExpireEvent(cell));
        assertFalse(output().isBlank(), "MysteryExpireEvent must produce output");
    }

    // ── CoinTossEvent is suppressed (returns blank) ──────────────────────────

    @Test @DisplayName("CoinTossEvent produces no console output (intentionally suppressed)")
    void coinTossSuppressed() {
        com.ludot.model.Piece piece = new com.ludot.model.Piece("R1", Colour.RED);
        bus.publish(new com.ludot.events.dice.CoinTossEvent(piece, com.ludot.model.Direction.CLOCKWISE));
        assertTrue(output().isBlank(),
                "CoinTossEvent must not produce any console output (suppressed in formatter)");
    }

    // ── multiple events through one bus ──────────────────────────────────────

    @Test @DisplayName("multiple events each produce their own output line")
    void multipleEventsProduceSeparateLines() {
        bus.publish(new DiceRollEvent(player, 3));
        bus.publish(new DiceRollEvent(player, 6));
        long lineCount = output().lines().filter(l -> !l.isBlank()).count();
        assertEquals(2, lineCount, "Two DiceRollEvents must produce exactly two output lines");
    }
}
