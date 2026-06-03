package com.ludot.engine;
import com.ludot.command.*;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.events.*;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.factory.*;
import com.ludot.model.*;
import com.ludot.model.state.*;
import com.ludot.rules.RuleEngine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("MoveExecutor")
class MoveExecutorTest {
    private MoveExecutor executor;
    private List<GameEvent> published;
    @BeforeEach void setUp() {
        Board.resetInstance();
        CellFactory.initializeBoard(Board.getInstance());
        published = new ArrayList<>();
        EventBus bus = new EventBus();
        bus.subscribe(published::add);
        FixedRandomProvider random = new FixedRandomProvider();
        RuleEngine rules = RuleEngineFactory.createLudoTRuleEngine(random);
        MysteryManager mm = new MysteryManager(random, Board.getInstance());
        executor = new MoveExecutor(Board.getInstance(), rules, mm, bus);
    }
    @AfterEach void tearDown() { Board.resetInstance(); }
    @Test @DisplayName("MoveFromBase publishes CoinTossEvent and MoveEvent") void fromBaseEvents() {
        Piece p = new Piece("R1", Colour.RED);
        p.setDirection(Direction.CLOCKWISE);
        executor.execute(new MoveFromBase(p));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof CoinTossEvent));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof MoveEvent));
    }
    @Test @DisplayName("CaptureMove publishes CaptureEvent") void captureEvents() {
        Piece att = new Piece("R1", Colour.RED); Piece vic = new Piece("B1", Colour.BLUE);
        Cell c1 = Board.getInstance().getCell(5); Cell c2 = Board.getInstance().getCell(8);
        c1.addPiece(att); att.setPosition(c1); att.setState(new OnBoardState()); att.setDirection(Direction.CLOCKWISE);
        c2.addPiece(vic); vic.setPosition(c2); vic.setState(new OnBoardState());
        executor.execute(new CaptureMove(att, vic, c2, 3));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof CaptureEvent));
    }
    @Test @DisplayName("CaptureMove via executor: attacker lands on target cell, victim sent to base")
    void captureFullPath() {
        Piece att = new Piece("R1", Colour.RED);
        Piece vic = new Piece("B1", Colour.BLUE);
        Cell c1 = Board.getInstance().getCell(5);
        Cell c2 = Board.getInstance().getCell(8);
        c1.addPiece(att); att.setPosition(c1); att.setState(new OnBoardState()); att.setDirection(Direction.CLOCKWISE);
        c2.addPiece(vic); vic.setPosition(c2); vic.setState(new OnBoardState());

        executor.execute(new CaptureMove(att, vic, c2, 3));

        assertEquals(c2, att.getPosition(), "Attacker must land on the target cell");
        assertFalse(c1.getPieces().contains(att), "Attacker must leave its source cell");
        assertNull(vic.getPosition(), "Victim must be sent to base (null position)");
        assertInstanceOf(AtBaseState.class, vic.getState(), "Victim must be in AtBaseState");
    }

    @Test @DisplayName("MoveOnBoard publishes HomeReachEvent on last home cell") void homeReachEvent() {
        Piece p = new Piece("R1", Colour.RED);
        List<Cell> hs = Board.getInstance().getHomeStraight(Colour.RED);
        Cell penultimate = hs.get(3);
        penultimate.addPiece(p); p.setPosition(penultimate); p.setState(new InHomeStraightState());
        executor.execute(new MoveOnBoard(p, 1));
        assertTrue(published.stream().anyMatch(ev -> ev instanceof HomeReachEvent));
    }
}