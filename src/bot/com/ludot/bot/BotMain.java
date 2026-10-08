package com.ludot.bot;

import com.ludot.protocol.ServerLink;
import com.ludot.protocol.Colour;
import java.util.Random;

public final class BotMain {

    private static final long GAME_TIME_LIMIT_SECONDS = 3_600;

    private BotMain() {
    }

    public static void main(String[] argv) {
        try {
            Args args = Args.parse(argv);
            Colour colour = Colour.valueOf(args.require("colour").toUpperCase());
            String name = args.get("name", "bot_" + colour.name().toLowerCase() + "_" + ProcessHandle.current().pid() % 1000);
            try (ServerLink link = ServerLink.connect(args.get("host", "localhost"), args.getInt("port", 5555))) {
                BotClient bot = new BotClient(link, args.require("room"), colour, args.getLong("delay", 800),
                        BotStrategies.forColour(colour, new Random()), line -> System.out.println(line));
                BotClient.Result result = bot.play(name, GAME_TIME_LIMIT_SECONDS);
                System.out.printf("Game over. %s: %d rolls, %d moves. Standings: %s%n",
                        colour, result.rolls(), result.moves(), result.standings());
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: BotMain --host H --port P --room R-0001 --colour RED|GREEN|YELLOW|BLUE [--delay ms] [--name n]");
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Bot stopped: " + e.getMessage());
            System.exit(1);
        }
    }
}
