package com.ludot.rules;

import com.ludot.TestBoard;
import com.ludot.command.Move;
import com.ludot.dice.FixedRandomProvider;
import com.ludot.engine.MoveExecutor;
import com.ludot.engine.MysteryManager;
import com.ludot.events.EventBus;
import com.ludot.factory.RuleEngineFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.state.OnBoardState;
import java.util.ArrayList;
import java.util.List;

final class RuleScenario {

    final Board board = TestBoard.create();
    final FixedRandomProvider random = new FixedRandomProvider();
    final RuleEngine engine;
    final EventBus bus = new EventBus();
    final List<Player> players = new ArrayList<>();
    final List<Object> events = new ArrayList<>();
    private final List<Piece> pieces = new ArrayList<>();

    RuleScenario(boolean ludoT) {
        engine = ludoT ? RuleEngineFactory.createLudoTRuleEngine(random) : RuleEngineFactory.createClassicRuleEngine(random);
        engine.setEventBus(bus);
        bus.subscribe(events::add);
    }

    static RuleScenario ludoT() {
        return new RuleScenario(true);
    }

    static RuleScenario classic() {
        return new RuleScenario(false);
    }

    Piece piece(String name, Colour colour, int cellId, Direction direction) {
        Piece piece = new Piece(name, colour, board);
        Cell cell = board.getCell(cellId);
        cell.addPiece(piece);
        piece.setPosition(cell);
        piece.setState(new OnBoardState());
        piece.setDirection(direction);
        pieces.add(piece);
        return piece;
    }

    Piece inStraight(String name, Colour colour, int index) {
        Piece piece = new Piece(name, colour, board);
        Cell cell = board.getHomeStraight(colour).get(index);
        cell.addPiece(piece);
        piece.setPosition(cell);
        piece.setState(new InHomeStraightState());
        piece.setDirection(Direction.CLOCKWISE);
        pieces.add(piece);
        return piece;
    }

    Piece atHome(String name, Colour colour) {
        Piece piece = new Piece(name, colour, board);
        Cell cell = board.getHomeStraight(colour).get(4);
        cell.addPiece(piece);
        piece.setPosition(cell);
        piece.setState(new com.ludot.model.state.AtHomeState());
        pieces.add(piece);
        return piece;
    }

    Piece inBase(String name, Colour colour) {
        Piece piece = new Piece(name, colour, board);
        piece.setState(new com.ludot.model.state.AtBaseState());
        pieces.add(piece);
        return piece;
    }

    List<Move> movesFor(Colour colour, int roll) {
        return engine.getValidMoves(playerOf(colour), roll);
    }

    Player playerOf(Colour colour) {
        List<Piece> mine = pieces.stream().filter(p -> p.getColour() == colour).toList();
        Player player = new Player(colour, mine, (state, r, ps, moves) -> moves.get(0));
        players.removeIf(p -> p.getColour() == colour);
        players.add(player);
        return player;
    }

    MoveExecutor executor() {
        playerOf(Colour.RED);
        for (Colour c : Colour.values()) {
            if (players.stream().noneMatch(p -> p.getColour() == c)) {
                playerOf(c);
            }
        }
        return new MoveExecutor(board, engine, new MysteryManager(random, board), bus, players);
    }

    Cell cell(int id) {
        return board.getCell(id);
    }

    Cell straight(Colour colour, int index) {
        return board.getHomeStraight(colour).get(index);
    }
}
