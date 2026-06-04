package com.ludot.rules.home;

import com.ludot.model.Piece;

// Template Method — defines the home-straight entry algorithm; subclasses override the
// meetsAdditionalRequirements hook to add variant-specific conditions (e.g., T-7 capture gate).
public class BaseHomeEntryRule {

    // final: the algorithm (at approach AND meets extra requirements) must not be bypassed.
    public final boolean canEnterHomeStraight(Piece piece) {
        return piece.canEnterHomeStraight() && meetsAdditionalRequirements(piece);
    }

    // protected hook: base game has no extra requirements; LudoT overrides to add the capture gate.
    protected boolean meetsAdditionalRequirements(Piece piece) {
        return true;
    }
}
