package com.ddev.TindaKart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fills the database with sample products, sales, and debts when tables are empty.
 */
public final class SampleDataSeeder {

    private SampleDataSeeder() {
    }

    public static void seedIfEmpty() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureSchema(conn);
            seedDebtCustomersIfEmpty(conn);
            DebtCustomerDirectory.syncLocalitiesToDatabase(conn);
            if (countRows(conn, "inventory") > 0) {
                return;
            }
            conn.setAutoCommit(false);
            try {
                seedInventory(conn);
                seedSales(conn);
                seedDebt(conn);
                conn.commit();
                System.out.println("TindaKart: sample data loaded successfully.");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            System.err.println("TindaKart: could not seed sample data — " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void ensureSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL UNIQUE,
                    quantity INTEGER NOT NULL,
                    price DOUBLE PRECISION NOT NULL,
                    status VARCHAR(50) NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id SERIAL PRIMARY KEY,
                    inventory_product_id INTEGER REFERENCES inventory(id),
                    quantity INTEGER NOT NULL,
                    total_price DOUBLE PRECISION NOT NULL,
                    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS debt (
                    id SERIAL PRIMARY KEY,
                    nickname VARCHAR(255) NOT NULL,
                    amount DECIMAL(12, 2) NOT NULL,
                    date_occurred DATE DEFAULT CURRENT_DATE
                )
                """);
            DebtCustomerRepository.ensureTable(conn);
        }
    }

    private static void seedDebtCustomersIfEmpty(Connection conn) throws SQLException {
        if (countRows(conn, "debt_customer") > 0) {
            return;
        }
        Object[][] profiles = {
            {"Myrna Abucay", "Myrna Abucay", "09622020517", "myrna.abucay@gmail.com", 82,
                    CebuLocalities.formatAddress("Poblacion Oriental", CebuLocalities.CONSOLACION),
                    "Regular customer since 2024."},
            {"Jana Aldiano", "Jana Aldiano", "09171234567", "jana.aldiano@gmail.com", 74,
                    CebuLocalities.formatAddress("Tayud", CebuLocalities.LILOAN),
                    "Prefers SMS reminders."},
            {"Arvin Wagas", "Arvin Wagas", "09958881234", "arvin.wagas@gmail.com", 68,
                    CebuLocalities.formatAddress("Casili", CebuLocalities.CONSOLACION),
                    "Sometimes pays in two installments."},
            {"Mona Canillo", "Mona Canillo", "09062334451", "mona.canillo@gmail.com", 91,
                    CebuLocalities.formatAddress("San Vicente", CebuLocalities.LILOAN),
                    "Excellent payment history."},
            {"Aling Rosa", "Aling Rosa", "09281230987", "rosa.mercado@gmail.com", 77,
                    CebuLocalities.formatAddress("Cansaga", CebuLocalities.CONSOLACION),
                    "Neighborhood suki."},
            {"Mang Juan", "Mang Juan", "09191112233", "juan.delacruz@gmail.com", 63,
                    CebuLocalities.formatAddress("Mulao", CebuLocalities.LILOAN),
                    "Retired. Remind politely via SMS."},
            {"Teresa Bautista", "Teresa Bautista", "09471239876", "teresa.bautista@gmail.com", 85,
                    CebuLocalities.formatAddress("Jugan", CebuLocalities.CONSOLACION),
                    "Pays after salary day."},
            {"Carlo Mendez", "Carlo Mendez", "09562347890", "carlo.mendez@gmail.com", 70,
                    CebuLocalities.formatAddress("Cotcot", CebuLocalities.LILOAN),
                    "Small eatery owner."},
            {"Nene Cruz", "Nene Cruz", "09345671234", "nene.cruz@gmail.com", 88,
                    CebuLocalities.formatAddress("Pitogo", CebuLocalities.CONSOLACION),
                    "Trusted neighbor."},
            {"Boyet Santos", "Boyet Santos", "09663881209", "boyet.santos@gmail.com", 59,
                    CebuLocalities.formatAddress("Yati", CebuLocalities.LILOAN),
                    "New credit account."},
        };
        String sql = """
            INSERT INTO debt_customer (nickname, full_name, phone, email, address, credit_score, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Object[] row : profiles) {
                pstmt.setString(1, (String) row[0]);
                pstmt.setString(2, (String) row[1]);
                pstmt.setString(3, (String) row[2]);
                pstmt.setString(4, (String) row[3]);
                pstmt.setString(5, (String) row[5]);
                pstmt.setInt(6, (Integer) row[4]);
                pstmt.setString(7, (String) row[6]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static int countRows(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void seedInventory(Connection conn) throws SQLException {
        Object[][] products = {
            {"Lucky Me Pancit Canton", 120, 15.00, "In Stock"},
            {"Coca-Cola 1.5L", 85, 65.00, "In Stock"},
            {"SkyFlakes Crackers", 200, 12.00, "In Stock"},
            {"Bear Brand Adult Plus 33g", 150, 18.00, "In Stock"},
            {"Tide Powder 180g", 90, 15.00, "In Stock"},
            {"Safeguard Soap 135g", 60, 38.00, "In Stock"},
            {"Nescafe 3-in-1 Original", 300, 8.00, "In Stock"},
            {"Clover Chips Cheese", 75, 20.00, "In Stock"},
            {"Minola Cooking Oil 1L", 40, 85.00, "In Stock"},
            {"Argentina Corned Beef", 55, 52.00, "In Stock"},
            {"Century Tuna Flakes", 70, 42.00, "In Stock"},
            {"Milo Chocolate Drink 22g", 180, 12.00, "In Stock"},
            {"Downy Fabric Conditioner 65ml", 45, 18.00, "In Stock"},
            {"Colgate Toothpaste 100ml", 35, 55.00, "In Stock"},
            {"Purefoods Hotdog 1kg", 25, 120.00, "In Stock"},
            {"Rebisco Crackers Fita", 0, 10.00, "Out of Stock"},
        };

        String sql = "INSERT INTO inventory (name, quantity, price, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Object[] row : products) {
                pstmt.setString(1, (String) row[0]);
                pstmt.setInt(2, (Integer) row[1]);
                pstmt.setDouble(3, (Double) row[2]);
                pstmt.setString(4, (String) row[3]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static void seedSales(Connection conn) throws SQLException {
        Map<String, Integer> productIds = loadProductIds(conn);
        LocalDate today = LocalDate.now();

        Object[][] sales = {
            {"Lucky Me Pancit Canton", 12, daysAgo(today, 0), 10, 30},
            {"Coca-Cola 1.5L", 8, daysAgo(today, 0), 14, 0},
            {"SkyFlakes Crackers", 25, daysAgo(today, 0), 9, 15},
            {"Nescafe 3-in-1 Original", 40, daysAgo(today, 0), 11, 0},

            {"Bear Brand Adult Plus 33g", 10, daysAgo(today, 1), 8, 30},
            {"Safeguard Soap 135g", 6, daysAgo(today, 1), 16, 0},
            {"Clover Chips Cheese", 15, daysAgo(today, 2), 10, 0},

            {"Minola Cooking Oil 1L", 5, daysAgo(today, 5), 9, 0},
            {"Argentina Corned Beef", 8, daysAgo(today, 5), 11, 30},
            {"Century Tuna Flakes", 12, daysAgo(today, 6), 13, 0},

            {"Lucky Me Pancit Canton", 20, daysAgo(today, 10), 10, 0},
            {"Coca-Cola 1.5L", 15, daysAgo(today, 12), 15, 30},
            {"Milo Chocolate Drink 22g", 30, daysAgo(today, 14), 9, 0},

            {"Tide Powder 180g", 18, daysAgo(today, 18), 11, 0},
            {"Purefoods Hotdog 1kg", 4, daysAgo(today, 20), 14, 0},
            {"Colgate Toothpaste 100ml", 7, daysAgo(today, 22), 10, 30},

            {"SkyFlakes Crackers", 35, daysAgo(today, 28), 9, 0},
            {"Nescafe 3-in-1 Original", 50, daysAgo(today, 30), 8, 0},
            {"Downy Fabric Conditioner 65ml", 10, daysAgo(today, 35), 13, 0},
        };

        String sql = """
            INSERT INTO sales (inventory_product_id, quantity, total_price, sale_date)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Object[] sale : sales) {
                String productName = (String) sale[0];
                Integer productId = productIds.get(productName);
                if (productId == null) {
                    continue;
                }
                int qty = (Integer) sale[1];
                LocalDate date = (LocalDate) sale[2];
                int hour = (Integer) sale[3];
                int minute = (Integer) sale[4];
                double unitPrice = lookupPrice(conn, productName);
                double total = unitPrice * qty;

                pstmt.setInt(1, productId);
                pstmt.setInt(2, qty);
                pstmt.setDouble(3, total);
                pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.of(date, LocalTime.of(hour, minute))));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static double lookupPrice(Connection conn, String productName) throws SQLException {
        String sql = "SELECT price FROM inventory WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
            }
        }
        return 0;
    }

    private static LocalDate daysAgo(LocalDate base, int days) {
        return base.minusDays(days);
    }

    private static Map<String, Integer> loadProductIds(Connection conn) throws SQLException {
        Map<String, Integer> ids = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name FROM inventory")) {
            while (rs.next()) {
                ids.put(rs.getString("name"), rs.getInt("id"));
            }
        }
        return ids;
    }

    private static void seedDebt(Connection conn) throws SQLException {
        LocalDate today = LocalDate.now();
        Object[][] debts = {
            {"Myrna Abucay", 350.00, today.minusDays(2)},
            {"Jana Aldiano", 125.50, today.minusDays(5)},
            {"Arvin Wagas", 480.00, today.minusDays(1)},
            {"Mona Canillo", 90.00, today.minusDays(8)},
            {"Aling Rosa", 215.00, today.minusDays(3)},
            {"Mang Juan", 60.00, today.minusDays(12)},
            {"Teresa Bautista", 175.25, today.minusDays(6)},
            {"Carlo Mendez", 320.00, today.minusDays(4)},
            {"Nene Cruz", 45.00, today.minusDays(15)},
            {"Boyet Santos", 510.00, today.minusDays(7)},
        };

        String sql = "INSERT INTO debt (nickname, amount, date_occurred) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Object[] row : debts) {
                pstmt.setString(1, (String) row[0]);
                pstmt.setDouble(2, (Double) row[1]);
                pstmt.setDate(3, java.sql.Date.valueOf((LocalDate) row[2]));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}
