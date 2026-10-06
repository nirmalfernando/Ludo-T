package com.ludot.server.adapters.jdbc;

/**
 * Where the database is. mode is "postgres" (Supabase), "h2" (the offline fallback, a separate H2 server
 * process speaking PostgreSQL mode) or "none" (keep everything in memory).
 */
public record DatabaseConfig(String mode, String url, String user, String password) {

    public static DatabaseConfig none() {
        return new DatabaseConfig("none", "", "", "");
    }

    public boolean enabled() {
        return !"none".equalsIgnoreCase(mode) && url != null && !url.isBlank();
    }
}
