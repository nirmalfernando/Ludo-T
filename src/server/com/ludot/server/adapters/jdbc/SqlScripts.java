package com.ludot.server.adapters.jdbc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs {@code .sql} files over JDBC, so setting up the database needs neither psql nor a web console.
 * Statements end with a semicolon and {@code --} starts a comment; a semicolon inside a string literal is not supported.
 */
public final class SqlScripts {

    private SqlScripts() {
    }

    /** Splits a script into its statements, dropping comments and blanks. */
    public static List<String> split(String script) {
        StringBuilder withoutComments = new StringBuilder();
        for (String line : script.split("\\R")) {
            int comment = line.indexOf("--");
            withoutComments.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        List<String> statements = new ArrayList<>();
        for (String part : withoutComments.toString().split(";")) {
            String statement = part.trim();
            if (!statement.isEmpty()) {
                statements.add(statement);
            }
        }
        return statements;
    }

    /** Executes every statement of the file, in order, each in its own transaction. */
    public static int run(Connection connection, Path file) throws IOException, SQLException {
        List<String> statements = split(Files.readString(file));
        connection.setAutoCommit(true);
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
        return statements.size();
    }
}
