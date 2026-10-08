package com.ludot.server.adapters.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// finds the highest room number already stored, so a restarted server carries on counting instead of reusing ids
public final class RoomNumbers {

    private static final String PREFIX = "R-";

    private RoomNumbers() {
    }

    public static long highest(Connection connection) throws SQLException {
        long highest = 0;
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id FROM rooms")) {
            while (rows.next()) {
                highest = Math.max(highest, parse(rows.getString(1)));
            }
        }
        return highest;
    }

    static long parse(String roomId) {
        if (roomId == null || !roomId.startsWith(PREFIX)) {
            return 0;
        }
        try {
            return Long.parseLong(roomId.substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
