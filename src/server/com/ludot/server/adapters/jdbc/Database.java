package com.ludot.server.adapters.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {

    private static final int LOGIN_TIMEOUT_SECONDS = 10;
    private static final int PG_SOCKET_TIMEOUT_SECONDS = 30;

    private final DatabaseConfig config;

    public Database(DatabaseConfig config) {
        this.config = config;
    }

    public DatabaseConfig config() {
        return config;
    }

    // throws if the database cannot be reached or the credentials are wrong
    public Connection open() throws SQLException {
        loadDriver(config.url());
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Properties properties = new Properties();
        properties.setProperty("user", config.user());
        properties.setProperty("password", config.password());
        if (config.url().startsWith("jdbc:postgresql:")) {
            // send consecutive INSERTs as one statement, and never hang forever on a dead network
            properties.setProperty("reWriteBatchedInserts", "true");
            properties.setProperty("socketTimeout", String.valueOf(PG_SOCKET_TIMEOUT_SECONDS));
        }
        return DriverManager.getConnection(config.url(), properties);
    }

    private static void loadDriver(String url) throws SQLException {
        String driver = url.startsWith("jdbc:postgresql:") ? "org.postgresql.Driver"
                : url.startsWith("jdbc:h2:") ? "org.h2.Driver" : null;
        if (driver == null) {
            return;
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver " + driver + " is not on the classpath (run download-tools)", e);
        }
    }
}
