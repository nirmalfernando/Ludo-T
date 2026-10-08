package com.ludot.bot;

import com.ludot.protocol.Colour;
import com.ludot.protocol.Speed;

public record StressOptions(String host, int port, String roomId, boolean create, String name, int requests,
                            int threads, Colour seat, int ratePerSecond, long startAtEpochMs, boolean startGame,
                            Speed speed, int waitForGameOverSeconds, long randomSeed) {

    public static StressOptions fromArgs(Args args) {
        boolean create = args.has("create");
        return new StressOptions(args.get("host", "localhost"), args.getInt("port", 5555),
                create ? null : args.require("room"), create, args.get("name", "stress"),
                args.getInt("requests", 2000), args.getInt("threads", 4),
                Colour.valueOf(args.get("seat", "RED").toUpperCase()), args.getInt("rate", 0),
                args.getLong("start-at", 0), args.has("start-game"),
                args.has("speed") ? Speed.valueOf(args.get("speed", "FAST").toUpperCase()) : null,
                args.getInt("wait", 120), args.getLong("random-seed", System.nanoTime()));
    }
}
