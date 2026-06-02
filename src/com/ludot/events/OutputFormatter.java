package com.ludot.events;

import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.block.BlockMoveEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.events.effect.EffectAppliedEvent;
import com.ludot.events.effect.EffectExpiredEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.events.home.HomeEntryEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.events.mystery.MysteryExpireEvent;
import com.ludot.events.mystery.MysterySpawnEvent;
import com.ludot.events.mystery.MysteryTeleportEvent;
import com.ludot.model.Cell;

// Converts each event type into a console-ready string via overloaded format() methods.
public class OutputFormatter {

    // dice
    public String format(DiceRollEvent e)  { return e.player().getColour() + " rolled " + e.roll(); }
    public String format(CoinTossEvent e)  { return e.piece().getName() + " direction assigned: " + e.direction(); }
    public String format(TripleSixEvent e) { return e.player().getColour() + " rolled triple six - turn forfeited"; }

    // movement
    public String format(MoveEvent e) {
        return e.piece().getName() + " moves from " + cellLabel(e.from()) + " to " + cellLabel(e.to());
    }
    public String format(CaptureEvent e) {
        return e.attacker().getName() + " captures " + e.victim().getName() + " at " + cellLabel(e.cell());
    }
    public String format(SkipTurnEvent e) { return e.player().getColour() + " has no valid moves - turn skipped"; }

    // block
    public String format(BlockFormEvent e)  { return "Block formed at " + cellLabel(e.cell()); }
    public String format(BlockMoveEvent e)  { return "Block moves from " + cellLabel(e.from()) + " to " + cellLabel(e.to()); }
    public String format(BlockBreakEvent e) { return "Block broken at " + cellLabel(e.cell()); }

    // home straight
    public String format(HomeEntryEvent e) { return e.piece().getName() + " enters home straight"; }
    public String format(HomeReachEvent e) { return e.piece().getName() + " reached home!"; }

    // mystery cell
    public String format(MysterySpawnEvent e) {
        return "Mystery cell spawned at " + cellLabel(e.cell()) + " (round " + e.roundCount() + ")";
    }
    public String format(MysteryExpireEvent e) { return "Mystery cell expired at " + cellLabel(e.cell()); }
    public String format(MysteryTeleportEvent e) {
        return e.piece().getName() + " teleported from " + cellLabel(e.from())
                + " to " + cellLabel(e.to()) + " [" + e.effectName() + "]";
    }

    // effects
    public String format(EffectAppliedEvent e) {
        return e.piece().getName() + " is now " + e.effectName() + " for " + e.duration() + " rounds";
    }
    public String format(EffectExpiredEvent e) {
        return e.piece().getName() + " " + e.effectName() + " effect expired";
    }

    // game flow
    public String format(RoundEndEvent e) { return "--- End of round " + e.roundNumber() + " ---"; }
    public String format(WinEvent e)      { return e.winner().getColour() + " wins in " + e.totalRounds() + " rounds!"; }

    // DRY helper — consistent cell label across all format methods
    private String cellLabel(Cell cell) {
        return cell != null ? "cell " + cell.getId() : "base";
    }
}
