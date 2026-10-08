package com.ludot.bot;

import com.ludot.protocol.Colour;
import com.ludot.protocol.MoveKind;
import com.ludot.protocol.MoveOption;
import java.util.Random;
import java.util.function.ToIntFunction;

public final class BotStrategies {

    private static final int SKIP_PENALTY = -1_000;

    private BotStrategies() {
    }

    public static BotStrategy forColour(Colour colour, Random random) {
        return switch (colour) {
            case RED -> scoring(option -> score(option, 100, 50, 20, 30, 5));
            case GREEN -> scoring(option -> score(option, 60, 50, 20, 30, 100));
            case YELLOW -> scoring(option -> score(option, 40, 100, 80, 30, 5));
            case BLUE -> options -> random.nextInt(options.size());
        };
    }

    private static int score(MoveOption option, int capture, int reachHome, int homeStraight,
                             int leaveBase, int formBlock) {
        if (option.kind() == MoveKind.SKIP) {
            return SKIP_PENALTY;
        }
        int score = 0;
        score += option.captures() ? capture : 0;
        score += option.reachesHome() ? reachHome : 0;
        score += option.entersHomeStraight() || option.to() == com.ludot.protocol.Location.HOME_STRAIGHT ? homeStraight : 0;
        score += option.kind() == MoveKind.FROM_BASE ? leaveBase : 0;
        score += option.formsBlock() ? formBlock : 0;
        return score;
    }

    private static BotStrategy scoring(ToIntFunction<MoveOption> scorer) {
        return options -> {
            int best = 0;
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < options.size(); i++) {
                int score = scorer.applyAsInt(options.get(i));
                if (score > bestScore) {
                    bestScore = score;
                    best = i;
                }
            }
            return best;
        };
    }
}
