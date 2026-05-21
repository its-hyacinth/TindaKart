/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ddev.TindaKart;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/tindakart";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "8080";

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
