package com.ludot.rules;

import java.util.List;
import com.ludot.command.Move;
import com.ludot.model.Cell;
import com.ludot.model.Piece;
import com.ludot.model.Player;

// Mediator that coordinates all rule objects and exposes a unified query API to the game engine.

public class RuleEngine {

    private final BaseMoveValidator moveValidator;
    private final BaseCaptureRule captureRule;
    private final BlockRule blockRule;
    private final BaseHomeEntryRule homeEntryRule;
    private final BaseDiceRule diceRule;
    private final MysteryRule mysteryRule;
    private final EffectRule effectRule;

    public RuleEngine(BaseMoveValidator moveValidator, BaseCaptureRule captureRule,
                      BlockRule blockRule, BaseHomeEntryRule homeEntryRule,
                      BaseDiceRule diceRule, MysteryRule mysteryRule, EffectRule effectRule) {
        this.moveValidator = moveValidator;
        this.captureRule = captureRule;
        this.blockRule = blockRule;
        this.homeEntryRule = homeEntryRule;
        this.diceRule = diceRule;
        this.mysteryRule = mysteryRule;
        this.effectRule = effectRule;
    }

    public List<Move> getValidMoves(Player player, int roll) {
        return List.of();
    }

    public RuleResult applyLandingEffects(Piece piece, Cell cell) {
        return null;
    }

    public int calculateEffectiveSteps(Piece piece, int roll) {
        return 0;
    }
}
