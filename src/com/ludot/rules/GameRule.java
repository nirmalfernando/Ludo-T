package com.ludot.rules;

// Strategy/Chain-of-Responsibility interface for a single validatable game rule.

public interface GameRule {

    boolean isApplicable(MoveContext context);

    RuleResult apply(MoveContext context);

    int getPriority();
}
