package com.ludot.events;

// Converts game events into console strings. Update these to match Section 3 of the spec exactly.
public class OutputFormatter {

    public String formatDiceRoll(DiceRollEvent e) {
        return e.player().getColour() + " rolled " + e.roll();
    }

    public String formatCoinToss(CoinTossEvent e) {
        return e.piece().getName() + " direction assigned: " + e.direction();
    }

    public String formatMove(MoveEvent e) {
        return e.piece().getName() + " moves from cell "
                + e.from().getId() + " to cell " + e.to().getId();
    }

    public String formatCapture(CaptureEvent e) {
        return e.attacker().getName() + " captures " + e.victim().getName()
                + " at cell " + e.cell().getId();
    }

    public String formatBlockForm(BlockFormEvent e) {
        return "Block formed at cell " + e.cell().getId();
    }

    public String formatBlockMove(BlockMoveEvent e) {
        return "Block moves from cell " + e.from().getId()
                + " to cell " + e.to().getId();
    }

    public String formatBlockBreak(BlockBreakEvent e) {
        return "Block broken at cell " + e.cell().getId();
    }

    public String formatHomeEntry(HomeEntryEvent e) {
        return e.piece().getName() + " enters home straight";
    }

    public String formatHomeReach(HomeReachEvent e) {
        return e.piece().getName() + " reached home!";
    }

    public String formatMysterySpawn(MysterySpawnEvent e) {
        return "Mystery cell spawned at cell " + e.cell().getId()
                + " (round " + e.roundCount() + ")";
    }

    public String formatMysteryExpire(MysteryExpireEvent e) {
        return "Mystery cell expired at cell " + e.cell().getId();
    }

    public String formatMysteryTeleport(MysteryTeleportEvent e) {
        return e.piece().getName() + " teleported from cell "
                + e.from().getId() + " to cell " + e.to().getId()
                + " [" + e.effectName() + "]";
    }

    public String formatEffectApplied(EffectAppliedEvent e) {
        return e.piece().getName() + " is now " + e.effectName()
                + " for " + e.duration() + " rounds";
    }

    public String formatEffectExpired(EffectExpiredEvent e) {
        return e.piece().getName() + " " + e.effectName() + " effect expired";
    }

    public String formatSkipTurn(SkipTurnEvent e) {
        return e.player().getColour() + " has no valid moves — turn skipped";
    }

    public String formatTripleSix(TripleSixEvent e) {
        return e.player().getColour() + " rolled triple six — turn forfeited";
    }

    public String formatRoundEnd(RoundEndEvent e) {
        return "--- End of round " + e.roundNumber() + " ---";
    }

    public String formatWin(WinEvent e) {
        return e.winner().getColour() + " wins in " + e.totalRounds() + " rounds!";
    }
}
