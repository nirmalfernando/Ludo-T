package com.ludot.command;
import com.ludot.model.Colour;
import com.ludot.model.Player;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("SkipMove")
class SkipMoveTest {
    private final Player player = new Player(Colour.RED, List.of(), (gs,r,ps,m)->m.get(0));
    @Test @DisplayName("execute returns successful result") void returnsSuccess() {
        assertTrue(new SkipMove(player).execute().isSuccess());
    }
    @Test @DisplayName("no side effects — no capture, no bonus") void noBoardChange() {
        MoveResult r = new SkipMove(player).execute();
        assertFalse(r.hasCaptured());
        assertFalse(r.grantsBonusRoll());
    }
}