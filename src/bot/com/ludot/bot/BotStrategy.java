package com.ludot.bot;

import com.ludot.protocol.MoveOption;
import java.util.List;

public interface BotStrategy {

    int choose(List<MoveOption> options);
}
