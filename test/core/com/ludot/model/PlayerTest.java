package com.ludot.model;
import com.ludot.model.state.AtBaseState;
import com.ludot.model.state.AtBriefingState;
import com.ludot.model.state.AtHomeState;
import com.ludot.model.state.InHomeStraightState;
import com.ludot.model.state.OnBoardState;
import com.ludot.model.state.PieceState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Player")
class PlayerTest {

    private Board board;
    private Player makePlayer(PieceState... states) {
        java.util.List<Piece> pieces = new java.util.ArrayList<>();
        for (PieceState s : states) {
            Piece p = new Piece("P", Colour.RED, board);
            p.setState(s);
            pieces.add(p);
        }
        return new Player(Colour.RED, pieces, (gs,r,ps,m)->m.get(0));
    }
    @Test @DisplayName("getPiecesAtBase is AtBaseState only: a piece in briefing is sitting on the board (cell 25)") void piecesAtBase() {
        Player p = makePlayer(new AtBaseState(), new AtBriefingState(4), new OnBoardState());
        assertEquals(1, p.getPiecesAtBase().size());
        assertEquals(2, p.getPiecesOnBoard().size(), "the OnBoard piece and the briefed piece");
    }
    @Test @DisplayName("getPiecesOnBoard includes OnBoardState and InHomeStraightState") void piecesOnBoard() {
        Player p = makePlayer(new OnBoardState(), new InHomeStraightState(), new AtBaseState());
        assertEquals(2, p.getPiecesOnBoard().size());
    }
    @Test @DisplayName("getPiecesAtHome includes AtHomeState only") void piecesAtHome() {
        Player p = makePlayer(new AtHomeState(), new OnBoardState(), new AtBaseState());
        assertEquals(1, p.getPiecesAtHome().size());
    }
    @Test @DisplayName("hasWon when all pieces are home") void hasWonAllHome() {
        Player p = makePlayer(new AtHomeState(), new AtHomeState());
        assertTrue(p.hasWon());
    }
    @Test @DisplayName("hasWon false when any piece not home") void notWonYet() {
        Player p = makePlayer(new AtHomeState(), new OnBoardState());
        assertFalse(p.hasWon());
    }
}