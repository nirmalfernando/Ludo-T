package com.ludot.engine;

import com.ludot.TestBoard;
import com.ludot.command.CaptureMove;
import com.ludot.command.MoveFromBase;
import com.ludot.command.MoveOnBoard;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.events.EventBus;
import com.ludot.events.GameEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.state.OnBoardState;
import com.ludot.rules.RuleEngine;
import com.ludot.rules.block.BlockRule;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MoveExecutor")
class MoveExecutorTest {

    private Board board;
    private MoveExecutor executor;
    private FixedRandomProvider random;
    private List<GameEvent> published;
    @BeforeEach void setUp() {
        board = TestBoard.create();
        published = new ArrayList<>();
        EventBus bus = new EventBus();
        bus.subscribe(published::add);
        random = new FixedRandomProvider();
        RuleEngine rules = RuleEngineFactory.createLudoTRuleEngine(random);
        MysteryManager mm = new MysteryManager(random, board);
        executor = new MoveExecutor(board, rules, mm, bus);
    }
        @Test @DisplayName("MoveFromBase publishes CoinTossEvent and MoveEvent") void fromBaseEvents() {
        Piece p = new Piece("R1", Colour.RED, board);
        p.setDirection(Direction.CLOCKWISE);
        executor.execute(new MoveFromBase(p));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof CoinTossEvent));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof MoveEvent));
    }
    @Test @DisplayName("CaptureMove publishes CaptureEvent") void captureEvents() {
        Piece att = new Piece("R1", Colour.RED, board); Piece vic = new Piece("B1", Colour.BLUE, board);
        Cell c1 = board.getCell(5); Cell c2 = board.getCell(8);
        c1.addPiece(att); att.setPosition(c1); att.setState(new OnBoardState()); att.setDirection(Direction.CLOCKWISE);
        c2.addPiece(vic); vic.setPosition(c2); vic.setState(new OnBoardState());
        executor.execute(new CaptureMove(att, vic, c2, 3));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof CaptureEvent));
    }
    @Test @DisplayName("CaptureMove via executor: attacker lands on target cell, victim sent to base")
    void captureFullPath() {
        Piece att = new Piece("R1", Colour.RED, board);
        Piece vic = new Piece("B1", Colour.BLUE, board);
        Cell c1 = board.getCell(5);
        Cell c2 = board.getCell(8);
        c1.addPiece(att); att.setPosition(c1); att.setState(new OnBoardState()); att.setDirection(Direction.CLOCKWISE);
        c2.addPiece(vic); vic.setPosition(c2); vic.setState(new OnBoardState());

        executor.execute(new CaptureMove(att, vic, c2, 3));

        assertEquals(c2, att.getPosition(), "Attacker must land on the target cell");
        assertFalse(c1.getPieces().contains(att), "Attacker must leave its source cell");
        assertNull(vic.getPosition(), "Victim must be sent to base (null position)");
        assertInstanceOf(AtBaseState.class, vic.getState(), "Victim must be in AtBaseState");
    }

    @Test @DisplayName("MoveOnBoard publishes HomeReachEvent on last home cell") void homeReachEvent() {
        Piece p = new Piece("R1", Colour.RED, board);
        List<Cell> hs = board.getHomeStraight(Colour.RED);
        Cell penultimate = hs.get(3);
        penultimate.addPiece(p); p.setPosition(penultimate); p.setState(new InHomeStraightState());
        executor.execute(new MoveOnBoard(p, 1));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof HomeReachEvent));
    }

    // ── T-5: block formation must save originalDirection ─────────────────────

    @Test @DisplayName("T-5: the direction a piece is given when it is placed on X is remembered as its original direction")
    void originalDirectionIsRememberedWhenLeavingBase() {
        Piece piece = new Piece("R1", Colour.RED, board);
        random.addCoins(false); // tails: counter-clockwise (T-1)

        executor.execute(new com.ludot.command.MoveFromBase(piece));

        assertEquals(Direction.COUNTER_CLOCKWISE, piece.getOriginalDirection(),
                "T-5: the original direction is the one it had when placed on X");
        piece.setDirection(Direction.CLOCKWISE); // later changes (Gamma, a block) do not alter what it reverts to
        assertEquals(Direction.COUNTER_CLOCKWISE, piece.getOriginalDirection());
    }

    @Test @DisplayName("T-5: full cycle — block forms, directions saved, break restores them")
    void blockFormationAndBreakCycle_T5() {
        Piece p1 = new Piece("R1", Colour.RED, board);
        Piece p2 = new Piece("R2", Colour.RED, board);
        Cell cell5 = board.getCell(5);
        Cell cell3 = board.getCell(3);

        cell5.addPiece(p1); p1.setPosition(cell5);
        p1.setState(new OnBoardState()); p1.setDirection(Direction.CLOCKWISE);
        cell3.addPiece(p2); p2.setPosition(cell3);
        p2.setState(new OnBoardState()); p2.setDirection(Direction.CLOCKWISE);

        executor.execute(new MoveOnBoard(p2, 2)); // forms block, originalDirections saved

        // simulate break (BlockRule.forceBreakBlock restores originalDirection)
        new BlockRule().forceBreakBlock(List.of(p1, p2));

        assertEquals(Direction.CLOCKWISE, p1.getDirection(), "T-5: p1 direction must be restored");
        assertEquals(Direction.CLOCKWISE, p2.getDirection(), "T-5: p2 direction must be restored");
        assertNull(p1.getOriginalDirection(), "T-5: originalDirection must be cleared after break");
        assertNull(p2.getOriginalDirection(), "T-5: originalDirection must be cleared after break");
    }
}