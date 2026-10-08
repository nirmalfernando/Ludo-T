package com.ludot.server.adapters.jdbc;

public record DatabaseConfig(String mode, String url, String user, String password) {

    public static DatabaseConfig none() {
        return new DatabaseConfig("none", "", "", "");
    }

    public boolean enabled() {
        return !"none".equalsIgnoreCase(mode) && url != null && !url.isBlank();
    }
}
