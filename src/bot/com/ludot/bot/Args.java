package com.ludot.bot;

import java.util.HashMap;
import java.util.Map;

/** Minimal {@code --key value} command-line parsing; a key with no value after it is a flag. */
public final class Args {

    private final Map<String, String> values = new HashMap<>();

    private Args() {
    }

    public static Args parse(String[] args) {
        Args parsed = new Args();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
            String key = arg.substring(2);
            boolean hasValue = i + 1 < args.length && !args[i + 1].startsWith("--");
            parsed.values.put(key, hasValue ? args[++i] : "true");
        }
        return parsed;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    /** A required option; throws with a helpful message if it is missing. */
    public String require(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required option --" + key);
        }
        return value;
    }

    public int getInt(String key, int fallback) {
        return values.containsKey(key) ? Integer.parseInt(values.get(key)) : fallback;
    }

    public long getLong(String key, long fallback) {
        return values.containsKey(key) ? Long.parseLong(values.get(key)) : fallback;
    }
}
