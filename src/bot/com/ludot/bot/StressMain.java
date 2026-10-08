package com.ludot.bot;

public final class StressMain {

    private StressMain() {
    }

    public static void main(String[] argv) {
        try {
            StressOptions options = StressOptions.fromArgs(Args.parse(argv));
            StressClient client = new StressClient(options, line -> System.out.println(line));
            StressReport report = client.run();
            System.out.println(report);
            System.exit(report.clean() ? 0 : 1);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: StressMain [--host H] [--port P] (--room R-0001 | --create) [--name n] [--seat RED]\n"
                    + "  [--requests 2000] [--threads 4] [--rate N] [--start-at epochMs] [--start-game]\n"
                    + "  [--speed fast|normal|slow] [--wait seconds]");
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Stress run failed: " + e);
            System.exit(1);
        }
    }
}
