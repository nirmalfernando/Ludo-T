package com.ludot.server.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Gives every thread a readable name (room-R-0007, scheduler-1, ...) so they can be told apart in a thread dump or VisualVM. */
public final class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final boolean numbered;
    private final AtomicInteger counter = new AtomicInteger(1);

    /** @param numbered append -1, -2, ... to the name; false names a single thread exactly {@code prefix}. */
    public NamedThreadFactory(String prefix, boolean numbered) {
        this.prefix = prefix;
        this.numbered = numbered;
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = new Thread(task, numbered ? prefix + "-" + counter.getAndIncrement() : prefix);
        thread.setDaemon(true);
        return thread;
    }
}
