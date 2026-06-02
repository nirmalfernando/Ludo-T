package com.ludot.rules.block;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
@DisplayName("BlockRule (T-3/4/5/6)")
class BlockRuleTest {
    @Test @DisplayName("isBlocked true for opponent block on destination") void opponentBlockBlocks() {}
    @Test @DisplayName("isBlocked false for friendly block") void friendlyBlockDoesNotBlock() {}
    @Test @DisplayName("canFormBlock true when same colour on cell") void canFormBlock() {}
    @Test @DisplayName("calcBlockSteps is roll / blockSize floor") void blockStepsFloorDiv() {}
    @Test @DisplayName("calcBlockMoveDir from first piece direction") void blockDirFromFirst() {}
    @Test @DisplayName("forceBreakBlock restores original directions") void restoresOriginalDir() {}
}