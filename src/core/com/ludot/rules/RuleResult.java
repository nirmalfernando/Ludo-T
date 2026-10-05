package com.ludot.rules;

public record RuleResult(boolean applicable, boolean success, String reason) {

    public static RuleResult notApplicable() {
        return new RuleResult(false, false, "Rule not applicable");
    }

    public static RuleResult success(String reason) {
        return new RuleResult(true, true, reason);
    }

    public static RuleResult failure(String reason) {
        return new RuleResult(true, false, reason);
    }
}
