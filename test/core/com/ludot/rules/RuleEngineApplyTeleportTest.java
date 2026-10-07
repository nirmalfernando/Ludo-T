package com.ludot.rules;

import com.ludot.TestBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.effects.EnergisedEffect;
import com.ludot.effects.SickEffect;
import com.ludot.effects.TeleportDestination;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.state.OnBoardState;
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

    @Test @DisplayName("T-14: GAMMA on a clockwise piece puts it on cell 44 and turns it counter-clockwise, with no briefing")
    void gammaCwFlipsToCcw() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.GAMMA);
        assertEquals(44, p.getPosition().getId(), "Gamma is the 46th cell from yellow's approach cell");
        assertEquals(Direction.COUNTER_CLOCKWISE, p.getDirection());
        assertInstanceOf(OnBoardState.class, p.getState(), "no briefing for a clockwise piece");
    }

    @Test @DisplayName("T-14: GAMMA on a counter-clockwise piece teleports it on to Beta (cell 25), where it attends a briefing")
    void gammaCcwGoesOnToBeta() {
        RuleEngine engine = engineWith(true);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.COUNTER_CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.GAMMA);
        assertEquals(25, p.getPosition().getId(), "teleported to Beta from Gamma");
        assertEquals(Direction.COUNTER_CLOCKWISE, p.getDirection(), "its direction is not changed");
        assertInstanceOf(AtBriefingState.class, p.getState());
    }

    // ── where Alpha, Beta and Gamma are (T-11) ───────────────────────────────

    @Test @DisplayName("T-11: Alpha is cell 7 and Beta is cell 25: the 9th and 27th cells from yellow's approach cell (50)")
    void alphaAndBetaAreRealCells() {
        RuleEngine engine = engineWith(true);
        Piece alpha = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(alpha, TeleportDestination.ALPHA);
        assertEquals(7, alpha.getPosition().getId());
        Piece beta = onBoardPiece(Colour.BLUE, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(beta, TeleportDestination.BETA);
        assertEquals(25, beta.getPosition().getId());
    }

    // ── what is on the destination cell ──────────────────────────────────────

    @Test @DisplayName("a lone opponent on the destination is captured by the arrival (Rule 6)")
    void teleportCapturesALoneOpponent() {
        RuleEngine engine = engineWith(true);
        Piece victim = onBoardPiece(Colour.BLUE, 7, Direction.CLOCKWISE);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        var captured = engine.applyMysteryEffect(p, TeleportDestination.ALPHA);
        assertEquals(java.util.Optional.of(victim), captured);
        assertNull(victim.getPosition());
        assertEquals(1, p.getCaptureCount());
    }

    @Test @DisplayName("an opposing block on the destination refuses the teleport: the piece stays where it is")
    void teleportRefusedByAnOpposingBlock() {
        RuleEngine engine = engineWith(true);
        onBoardPiece(Colour.BLUE, 25, Direction.CLOCKWISE);
        onBoardPiece(Colour.BLUE, 25, Direction.CLOCKWISE);
        Piece p = onBoardPiece(Colour.RED, 10, Direction.CLOCKWISE);
        engine.applyMysteryEffect(p, TeleportDestination.BETA);
        assertEquals(10, p.getPosition().getId());
        assertInstanceOf(OnBoardState.class, p.getState(), "no briefing either: the teleport did not happen");
    }

    @Test @DisplayName("T-15: landing on Alpha, Beta or Gamma without a teleport has no effect")
    void landingOnTheCellsHasNoEffect() {
        RuleScenario game = RuleScenario.ludoT();
        Piece piece = game.piece("R1", Colour.RED, 4, Direction.CLOCKWISE);
        game.executor().execute(game.movesFor(Colour.RED, 3).get(0), game.playerOf(Colour.RED)); // 4 -> 7 (Alpha)
        assertEquals(7, piece.getPosition().getId());
        assertInstanceOf(com.ludot.effects.NoEffect.class, piece.getActiveEffect());
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
