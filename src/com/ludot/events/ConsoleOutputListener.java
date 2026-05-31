package com.ludot.events;

// Receives every game event and delegates formatting to OutputFormatter before printing.
public class ConsoleOutputListener implements GameEventListener {

    private final OutputFormatter formatter;

    public ConsoleOutputListener(OutputFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void onEvent(GameEvent event) {
        String line = null;

        if      (event instanceof DiceRollEvent e)        line = formatter.formatDiceRoll(e);
        else if (event instanceof CoinTossEvent e)        line = formatter.formatCoinToss(e);
        else if (event instanceof MoveEvent e)            line = formatter.formatMove(e);
        else if (event instanceof CaptureEvent e)         line = formatter.formatCapture(e);
        else if (event instanceof BlockFormEvent e)       line = formatter.formatBlockForm(e);
        else if (event instanceof BlockMoveEvent e)       line = formatter.formatBlockMove(e);
        else if (event instanceof BlockBreakEvent e)      line = formatter.formatBlockBreak(e);
        else if (event instanceof HomeEntryEvent e)       line = formatter.formatHomeEntry(e);
        else if (event instanceof HomeReachEvent e)       line = formatter.formatHomeReach(e);
        else if (event instanceof MysterySpawnEvent e)    line = formatter.formatMysterySpawn(e);
        else if (event instanceof MysteryExpireEvent e)   line = formatter.formatMysteryExpire(e);
        else if (event instanceof MysteryTeleportEvent e) line = formatter.formatMysteryTeleport(e);
        else if (event instanceof EffectAppliedEvent e)   line = formatter.formatEffectApplied(e);
        else if (event instanceof EffectExpiredEvent e)   line = formatter.formatEffectExpired(e);
        else if (event instanceof SkipTurnEvent e)        line = formatter.formatSkipTurn(e);
        else if (event instanceof TripleSixEvent e)       line = formatter.formatTripleSix(e);
        else if (event instanceof RoundEndEvent e)        line = formatter.formatRoundEnd(e);
        else if (event instanceof WinEvent e)             line = formatter.formatWin(e);

        if (line != null) System.out.println(line);
    }
}
