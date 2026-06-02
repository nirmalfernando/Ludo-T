package com.ludot.strategy;
import com.ludot.command.*;
import com.ludot.model.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("BlueStrategy — cycling")
class BlueStrategyTest {
    private final Player player = new Player(Colour.BLUE, List.of(), (gs,r,ps,m)->m.get(0));
    @Test @DisplayName("cycles through moves by index") void cyclesThroughMoves() {
        BlueStrategy s = new BlueStrategy();
        Move m1 = new SkipMove(player);
        Move m2 = new SkipMove(player);
        List<Move> moves = List.of(m1, m2);
        assertSame(m1, s.decideMoves(null, 1, List.of(), moves));
        assertSame(m2, s.decideMoves(null, 1, List.of(), moves));
    }
    @Test @DisplayName("index wraps when exceeds list size") void indexWrapsAround() {
        BlueStrategy s = new BlueStrategy();
        Move m1 = new SkipMove(player);
        List<Move> moves = List.of(m1);
        s.decideMoves(null, 1, List.of(), moves);
        assertSame(m1, s.decideMoves(null, 1, List.of(), moves)); // wraps back to m1
    }
    @Test @DisplayName("index increments each call") void indexIncrementsEachCall() {
        BlueStrategy s = new BlueStrategy();
        Move m1 = new SkipMove(player);
        Move m2 = new SkipMove(player);
        Move m3 = new SkipMove(player);
        List<Move> moves = List.of(m1, m2, m3);
        assertSame(m1, s.decideMoves(null,1,List.of(),moves));
        assertSame(m2, s.decideMoves(null,1,List.of(),moves));
        assertSame(m3, s.decideMoves(null,1,List.of(),moves));
        assertSame(m1, s.decideMoves(null,1,List.of(),moves)); // wraps
    }
}