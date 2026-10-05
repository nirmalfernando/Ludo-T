package com.ludot.bot;

import com.ludot.protocol.MoveOption;
import java.util.List;

/** Picks one of the legal moves the server offered. */
public interface BotStrategy {

    /** @return the index (within the list) of the chosen option */
    int choose(List<MoveOption> options);
}
