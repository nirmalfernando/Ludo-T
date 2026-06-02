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

// Observer + Visitor: receives every event via EventBus and dispatches via accept() — no instanceof.
public class ConsoleOutputListener implements GameEventListener, EventVisitor {

    private final OutputFormatter formatter;

    public ConsoleOutputListener(OutputFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void onEvent(GameEvent event) {
        event.accept(this); // Visitor double-dispatch — routes to the correct visit() below
    }

    // dice
    @Override public void visit(DiceRollEvent e)  { print(formatter.format(e)); }
    @Override public void visit(CoinTossEvent e)  { print(formatter.format(e)); }
    @Override public void visit(TripleSixEvent e) { print(formatter.format(e)); }

    // movement
    @Override public void visit(MoveEvent e)     { print(formatter.format(e)); }
    @Override public void visit(CaptureEvent e)  { print(formatter.format(e)); }
    @Override public void visit(SkipTurnEvent e) { print(formatter.format(e)); }

    // block
    @Override public void visit(BlockFormEvent e)  { print(formatter.format(e)); }
    @Override public void visit(BlockMoveEvent e)  { print(formatter.format(e)); }
    @Override public void visit(BlockBreakEvent e) { print(formatter.format(e)); }

    // home straight
    @Override public void visit(HomeEntryEvent e) { print(formatter.format(e)); }
    @Override public void visit(HomeReachEvent e) { print(formatter.format(e)); }

    // mystery cell
    @Override public void visit(MysterySpawnEvent e)   { print(formatter.format(e)); }
    @Override public void visit(MysteryExpireEvent e)  { print(formatter.format(e)); }
    @Override public void visit(MysteryTeleportEvent e){ print(formatter.format(e)); }

    // effects
    @Override public void visit(EffectAppliedEvent e) { print(formatter.format(e)); }
    @Override public void visit(EffectExpiredEvent e) { print(formatter.format(e)); }

    // game flow
    @Override public void visit(RoundEndEvent e) { print(formatter.format(e)); }
    @Override public void visit(WinEvent e)      { print(formatter.format(e)); }

    private void print(String line) {
        System.out.println(line);
    }
}
