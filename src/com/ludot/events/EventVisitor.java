package com.ludot.events;

import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.block.BlockMoveEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.events.effect.EffectAppliedEvent;
import com.ludot.events.effect.EffectExpiredEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.events.home.HomeEntryEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.events.mystery.MysteryExpireEvent;
import com.ludot.events.mystery.MysterySpawnEvent;
import com.ludot.events.mystery.MysteryTickEvent;
import com.ludot.events.mystery.MysteryTeleportEvent;

// Visitor pattern — one visit() per event type; implementors handle each event with full type safety.
public interface EventVisitor {

    // dice
    void visit(DiceRollEvent e);
    void visit(CoinTossEvent e);
    void visit(TripleSixEvent e);

    // movement
    void visit(MoveEvent e);
    void visit(CaptureEvent e);
    void visit(SkipTurnEvent e);

    // block
    void visit(BlockFormEvent e);
    void visit(BlockMoveEvent e);
    void visit(BlockBreakEvent e);

    // home straight
    void visit(HomeEntryEvent e);
    void visit(HomeReachEvent e);

    // mystery cell
    void visit(MysterySpawnEvent e);
    void visit(MysteryTickEvent e);
    void visit(MysteryExpireEvent e);
    void visit(MysteryTeleportEvent e);

    // effects
    void visit(EffectAppliedEvent e);
    void visit(EffectExpiredEvent e);

    // game flow
    void visit(RollOffEvent e);
    void visit(TurnOrderEvent e);
    void visit(RoundEndEvent e);
    void visit(WinEvent e);
}
