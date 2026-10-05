package com.ludot.rules;

import com.ludot.TestBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.effects.EnergisedEffect;
import com.ludot.effects.NoEffect;
import com.ludot.effects.SickEffect;
import com.ludot.effects.TeleportDestination;
import com.ludot.factory.CellFactory;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.state.OnBoardState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuleEngine.applyMysteryEffect — all 6 TeleportDestination branches (T-12/13/14)")
class RuleEngineApplyTeleportTest {

    private Board board;

    @BeforeEach void setUp() {
        board = TestBoard.create();
        board = board;
    }

    
    private Piece onBoardPiece(Colour colour, int cellId, Direction dir) {
        Piece p = new Piece(colour.name().charAt(0) + "1", colour, board);
        Cell cell = board.getCell(cellId);
        cell.addPiece(p);
        p.setPosition(cell);
        p.setState(new OnBoardState());
        p.setDirection(dir);
        return p;
    }

    private RuleEngine engineWith(boolean alphaOutcome) {
        FixedRandomProvider random = new FixedRandomProvider().addAlphaOutcomes(alphaOutcome);
        return RuleEngineFactory.createLudoTRuleEngine(random);
    }

    // ── ALPHA ────────────────────────────────────────────────────────────────

    @Test @DisplayName("ALPHA with heads outcome → piece receives EnergisedEffect (T-12)")
    void alphaEnergised() {
        RuleEngine engine = engineWith(true); // true = energised
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.ALPHA);
        assertInstanceOf(EnergisedEffect.class, p.getActiveEffect(),
                "ALPHA(heads) must apply EnergisedEffect");
    }

    @Test @DisplayName("ALPHA with tails outcome → piece receives SickEffect (T-12)")
    void alphaSick() {
        RuleEngine engine = engineWith(false); // false = sick
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.ALPHA);
        assertInstanceOf(SickEffect.class, p.getActiveEffect(),
                "ALPHA(tails) must apply SickEffect");
    }

    // ── BETA ─────────────────────────────────────────────────────────────────

    @Test @DisplayName("BETA → piece is locked in AtBriefingState (T-13)")
    void betaLocksInBriefing() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.BETA);
        assertInstanceOf(AtBriefingState.class, p.getState(),
                "BETA must transition piece to AtBriefingState");
    }

    // ── GAMMA ─────────────────────────────────────────────────────────────────

    @Test @DisplayName("GAMMA on CW piece flips direction to CCW and triggers Beta (T-14)")
    void gammaCwFlipsToCcwAndBeta() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.GAMMA);
        assertEquals(Direction.COUNTER_CLOCKWISE, p.getDirection(),
                "GAMMA on CW piece must flip to CCW");
        assertInstanceOf(AtBriefingState.class, p.getState(),
                "GAMMA CW→CCW must also apply Beta (briefing)");
    }

    @Test @DisplayName("GAMMA on CCW piece flips direction to CW only — no Beta (T-14)")
    void gammaCcwFlipsToCwNoBeta() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.COUNTER_CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.GAMMA);
        assertEquals(Direction.CLOCKWISE, p.getDirection(),
                "GAMMA on CCW piece must flip to CW");
        assertInstanceOf(OnBoardState.class, p.getState(),
                "GAMMA CCW→CW must NOT apply Beta");
    }

    // ── BASE ─────────────────────────────────────────────────────────────────

    @Test @DisplayName("BASE → piece returned to base with null position (T-11)")
    void baseSendsToBase() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.BASE);
        assertNull(p.getPosition(), "BASE must set piece position to null");
        assertInstanceOf(AtBaseState.class, p.getState(),
                "BASE must transition piece to AtBaseState");
    }

    // ── START_CELL ────────────────────────────────────────────────────────────

    @Test @DisplayName("START_CELL → piece teleports to its colour's start cell")
    void startCellTeleportsToStart() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.START_CELL);
        Cell redStart = board.getStartCell(Colour.RED);
        assertEquals(redStart, p.getPosition(),
                "START_CELL must teleport piece to its own start cell (cell 26 for RED)");
    }

    // ── APPROACH_CELL ─────────────────────────────────────────────────────────

    @Test @DisplayName("APPROACH_CELL → piece teleports to its colour's approach cell")
    void approachCellTeleportsToApproach() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.APPROACH_CELL);
        Cell redApproach = board.getApproachCell(Colour.RED);
        assertEquals(redApproach, p.getPosition(),
                "APPROACH_CELL must teleport piece to its own approach cell (cell 24 for RED)");
    }
}
