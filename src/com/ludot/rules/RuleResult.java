package com.ludot.rules;

import com.ludot.model.Piece;

// Immutable result returned by each rule in the Rule Chain; carries validity and side-effect data.

public class RuleResult {

    private final boolean valid;
    private final String reason;
    private final Piece captured;
    private final boolean grantsBonusRoll;

    public RuleResult(boolean valid, String reason, Piece captured, boolean grantsBonusRoll) {
        this.valid = valid;
        this.reason = reason;
        this.captured = captured;
        this.grantsBonusRoll = grantsBonusRoll;
    }

    public static RuleResult valid() {
        return new RuleResult(true, null, null, false);
    }

    public static RuleResult invalid(String reason) {
        return new RuleResult(false, reason, null, false);
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }

    public Piece getCaptured() {
        return captured;
    }

    public boolean grantsBonusRoll() {
        return grantsBonusRoll;
    }
}
