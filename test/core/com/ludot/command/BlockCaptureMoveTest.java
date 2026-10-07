package com.ludot.command;

import com.ludot.TestBoard;
import com.ludot.factory.CellFactory;
import com.ludot.model.Board;
import com.ludot.model.Cell;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.state.OnBoardState;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlockCaptureMove (T-8)")
class BlockCaptureMoveTest {

    private Board board;

    private Cell source, target;
    private Piece a1, a2, v1, v2;

    @BeforeEach void setUp() {
        board = TestBoard.create();
        source = board.getCell(5);
        target = board.getCell(10);

        a1 = new Piece("R1", Colour.RED, board); a2 = new Piece("R2", Colour.RED, board);
        v1 = new Piece("B1", Colour.BLUE, board); v2 = new Piece("B2", Colour.BLUE, board);

        source.addPiece(a1); a1.setPosition(source); a1.setState(new OnBoardState()); a1.setDirection(Direction.CLOCKWISE);
        source.addPiece(a2); a2.setPosition(source); a2.setState(new OnBoardState()); a2.setDirection(Direction.CLOCKWISE);
        target.addPiece(v1); v1.setPosition(target); v1.setState(new OnBoardState());
        target.addPiece(v2); v2.setPosition(target); v2.setState(new OnBoardState());
    }

    
    private BlockCaptureMove move() {
        return new BlockCaptureMove(List.of(a1, a2), List.of(v1, v2), target, 5);
    }

    @Test @DisplayName("all attackers move to the target cell") void attackersMoveToTarget() {
        move().execute();
        assertEquals(target, a1.getPosition());
        assertEquals(target, a2.getPosition());
    }

    @Test @DisplayName("source cell is empty after move") void sourceCellEmptied() {
        move().execute();
        assertFalse(source.isOccupied());
    }

    @Test @DisplayName("T-8: execute() leaves the capture counts alone; MoveExecutor credits each attacker once via the capture rule")
    void captureCountsAreCreditedByTheExecutor() {
        move().execute();
        assertEquals(0, a1.getCaptureCount(), "crediting happens once per victim in MoveExecutor, not here");
        assertEquals(0, a2.getCaptureCount(), "crediting happens once per victim in MoveExecutor, not here");
    }

    @Test @DisplayName("T-8: victim reset is NOT done inside execute() — that belongs to CaptureRule in MoveExecutor")
    void victimResetDelegatedToCaptureRule() {
        // After execute(), victims still have their positions — MoveExecutor calls captureRule.capture() separately.
        move().execute();
        assertNotNull(v1.getPosition(), "BlockCaptureMove.execute() must NOT reset victims directly");
        assertNotNull(v2.getPosition(), "BlockCaptureMove.execute() must NOT reset victims directly");
    }

    @Test @DisplayName("MoveResult grants bonus roll (T-8 capture grants T-2 bonus)") void grantsBonusRoll() {
        MoveResult result = move().execute();
        assertTrue(result.grantsBonusRoll(), "T-8 block capture must grant a bonus roll (T-2)");
    }

    @Test @DisplayName("describe returns a non-blank description") void describeNonBlank() {
        assertFalse(move().describe().isBlank());
    }
}
