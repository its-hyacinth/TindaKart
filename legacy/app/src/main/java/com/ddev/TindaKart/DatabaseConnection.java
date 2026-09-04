/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ddev.TindaKart;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

public class DatabaseConnection {
    private static final Map<String, String> DOT_ENV = loadDotEnv();
    private static final String DB_HOST = setting("DB_HOST", "localhost");
    private static final String DB_PORT = setting("DB_PORT", "5432");
    private static final String DB_NAME = setting("DB_NAME", "tindakart");
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
    private static final String DB_USER = setting("DB_USER", "postgres");
    private static final String DB_PASSWORD = setting("DB_PASSWORD", "");

    private static String setting(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = DOT_ENV.get(key);
        }
        return value == null ? fallback : value;
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (java.io.IOException ex) {
            System.err.println("TindaKart: could not read .env: " + ex.getMessage());
        }
        return values;
    }

    /**
     * Establishes a connection to the PostgreSQL database.
     * 
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static boolean canConnect() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    public static void showConnectionError() {
        JOptionPane.showMessageDialog(
                null,
                "Unable to connect to PostgreSQL.\n"
                + "Please make sure PostgreSQL is running and database settings are correct.\n\n"
                + "Expected:\n"
                + "- Host: localhost\n"
                + "- Port: 5432\n"
                + "- Database: tindakart\n"
                + "- User: postgres",
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
