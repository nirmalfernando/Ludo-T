package com.ludot.rules;

public interface GameRule {
    boolean isApplicable(MoveContext ctx);

    RuleResult apply(MoveContext ctx);

    int getPriority();
}
