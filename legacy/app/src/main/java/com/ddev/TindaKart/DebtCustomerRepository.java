package com.ddev.TindaKart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Optional;

public final class DebtCustomerRepository {

    private DebtCustomerRepository() {
    }

    public static void ensureTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS debt_customer (
                    id SERIAL PRIMARY KEY,
                    nickname VARCHAR(255) NOT NULL UNIQUE,
                    full_name VARCHAR(255) NOT NULL,
                    phone VARCHAR(32),
                    email VARCHAR(255),
                    address VARCHAR(500),
                    credit_score INTEGER NOT NULL DEFAULT 70,
                    notes TEXT
                )
                """);
        }
    }

    public static Optional<DebtCustomerProfile> load(String nickname) throws SQLException {
        String sql = """
            SELECT full_name, phone, email, address, credit_score, notes
            FROM debt_customer WHERE LOWER(TRIM(nickname)) = LOWER(TRIM(?))
            """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new DebtCustomerProfile(
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getInt("credit_score"),
                            rs.getString("address"),
                            rs.getString("notes")));
                }
            }
        }
        return Optional.empty();
    }

    public static void save(String nickname, DebtCustomerProfile profile) throws SQLException {
        String sql = """
            INSERT INTO debt_customer (nickname, full_name, phone, email, address, credit_score, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (nickname) DO UPDATE SET
                full_name = EXCLUDED.full_name,
                phone = EXCLUDED.phone,
                email = EXCLUDED.email,
                address = EXCLUDED.address,
                credit_score = EXCLUDED.credit_score,
                notes = EXCLUDED.notes
            """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nickname.trim());
            pstmt.setString(2, profile.fullName());
            pstmt.setString(3, profile.phone());
            pstmt.setString(4, profile.email());
            pstmt.setString(5, profile.address());
            pstmt.setInt(6, profile.creditScore());
            pstmt.setString(7, profile.notes());
            pstmt.executeUpdate();
        }
    }

    public static void renameAndSave(String oldNickname, String newNickname, DebtCustomerProfile profile)
            throws SQLException {
        String oldKey = oldNickname.trim();
        String newKey = newNickname.trim();
        if (oldKey.equalsIgnoreCase(newKey)) {
            save(oldKey, profile);
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            String delete = "DELETE FROM debt_customer WHERE LOWER(TRIM(nickname)) = LOWER(TRIM(?))";
            try (PreparedStatement pstmt = conn.prepareStatement(delete)) {
                pstmt.setString(1, oldKey);
                pstmt.executeUpdate();
            }
            save(newKey, profile);
        }
    }

    public static void updateDebtRecord(int debtId, String nickname, double amount) throws SQLException {
        String sql = "UPDATE debt SET nickname = ?, amount = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            pstmt.setDouble(2, amount);
            pstmt.setInt(3, debtId);
            pstmt.executeUpdate();
        }
    }

    public static String normalizeKey(String nickname) {
        return nickname.trim().toLowerCase(Locale.ROOT);
    }
}
