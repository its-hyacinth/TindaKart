package com.ddev.TindaKart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves customer profiles from database, with built-in defaults as fallback.
 */
public final class DebtCustomerDirectory {

    private static final Map<String, DebtCustomerProfile> DEFAULTS = new LinkedHashMap<>();

    static {
        registerDefault("Myrna Abucay", "09622020517", "myrna.abucay@gmail.com", 82,
                CebuLocalities.formatAddress("Poblacion Oriental", CebuLocalities.CONSOLACION),
                "Regular customer since 2024. Pays on the 15th.");
        registerDefault("Jana Aldiano", "09171234567", "jana.aldiano@gmail.com", 74,
                CebuLocalities.formatAddress("Tayud", CebuLocalities.LILOAN),
                "Prefers SMS reminders before due date.");
        registerDefault("Jona Aldiano", "09171234567", "jana.aldiano@gmail.com", 74,
                CebuLocalities.formatAddress("Tayud", CebuLocalities.LILOAN),
                "Prefers SMS reminders before due date.");
        registerDefault("Arvin Wagas", "09958881234", "arvin.wagas@gmail.com", 68,
                CebuLocalities.formatAddress("Casili", CebuLocalities.CONSOLACION),
                "Sometimes pays in two installments.");
        registerDefault("Arvin Wogas", "09958881234", "arvin.wagas@gmail.com", 68,
                CebuLocalities.formatAddress("Casili", CebuLocalities.CONSOLACION),
                "Sometimes pays in two installments.");
        registerDefault("Mona Canillo", "09062334451", "mona.canillo@gmail.com", 91,
                CebuLocalities.formatAddress("San Vicente", CebuLocalities.LILOAN),
                "Excellent payment history.");
        registerDefault("Aling Rosa", "09281230987", "rosa.mercado@gmail.com", 77,
                CebuLocalities.formatAddress("Cansaga", CebuLocalities.CONSOLACION),
                "Neighborhood suki. Buys on credit weekly.");
        registerDefault("Mang Juan", "09191112233", "juan.delacruz@gmail.com", 63,
                CebuLocalities.formatAddress("Mulao", CebuLocalities.LILOAN),
                "Retired. Remind politely via SMS.");
        registerDefault("Teresa Bautista", "09471239876", "teresa.bautista@gmail.com", 85,
                CebuLocalities.formatAddress("Jugan", CebuLocalities.CONSOLACION),
                "Office worker. Pays after salary day.");
        registerDefault("Carlo Mendez", "09562347890", "carlo.mendez@gmail.com", 70,
                CebuLocalities.formatAddress("Cotcot", CebuLocalities.LILOAN),
                "Small eatery owner. Bulk buyer.");
        registerDefault("Nene Cruz", "09345671234", "nene.cruz@gmail.com", 88,
                CebuLocalities.formatAddress("Pitogo", CebuLocalities.CONSOLACION),
                "Trusted neighbor.");
        registerDefault("Boyet Santos", "09663881209", "boyet.santos@gmail.com", 59,
                CebuLocalities.formatAddress("Yati", CebuLocalities.LILOAN),
                "New credit account. Monitor closely.");
    }

    private DebtCustomerDirectory() {
    }

    private static void registerDefault(String name, String phone, String email, int creditScore,
            String address, String notes) {
        DEFAULTS.put(normalize(name), new DebtCustomerProfile(name, phone, email, creditScore, address, notes));
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public static DebtCustomerProfile resolve(String nickname) {
        try {
            DebtCustomerRepository.ensureTable(DatabaseConnection.getConnection());
            Optional<DebtCustomerProfile> saved = DebtCustomerRepository.load(nickname);
            if (saved.isPresent()) {
                return saved.get();
            }
        } catch (SQLException ex) {
            System.err.println("Could not load customer profile: " + ex.getMessage());
        }

        DebtCustomerProfile known = DEFAULTS.get(normalize(nickname));
        if (known != null) {
            return known;
        }
        return generateDefault(nickname);
    }

    /** Updates stored profiles to Cebu addresses and @gmail.com emails. */
    public static void syncLocalitiesToDatabase(Connection conn) throws SQLException {
        Set<String> saved = new HashSet<>();
        for (DebtCustomerProfile profile : DEFAULTS.values()) {
            if (!saved.add(profile.fullName())) {
                continue;
            }
            DebtCustomerRepository.save(profile.fullName(), profile);
        }

        try (PreparedStatement fixEmail = conn.prepareStatement("""
                UPDATE debt_customer
                SET email = REPLACE(LOWER(email), '@email.com', '@gmail.com')
                WHERE LOWER(email) LIKE '%@email.com'
                """)) {
            fixEmail.executeUpdate();
        }

        String select = "SELECT nickname, full_name, phone, email, address, credit_score, notes FROM debt_customer";
        try (PreparedStatement pstmt = conn.prepareStatement(select);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String nickname = rs.getString("nickname");
                String email = rs.getString("email");
                String address = rs.getString("address");
                boolean needsGmail = email == null || !CebuLocalities.isGmailAddress(email);
                boolean needsCebu = address == null
                        || (!address.contains("Consolacion, Cebu") && !address.contains("Liloan, Cebu"));
                if (!needsGmail && !needsCebu) {
                    continue;
                }
                String newEmail = needsGmail ? CebuLocalities.gmailForName(rs.getString("full_name")) : email;
                String newAddress = needsCebu ? CebuLocalities.addressForCustomer(nickname) : address;
                DebtCustomerProfile updated = new DebtCustomerProfile(
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        newEmail,
                        rs.getInt("credit_score"),
                        newAddress,
                        rs.getString("notes"));
                DebtCustomerRepository.save(nickname, updated);
            }
        }
    }

    private static DebtCustomerProfile generateDefault(String nickname) {
        int hash = Math.abs(nickname.hashCode());
        int score = 60 + (hash % 35);
        String phone = String.format("09%09d", Math.abs(hash % 1_000_000_000));
        if (phone.length() > 11) {
            phone = phone.substring(0, 11);
        }
        return new DebtCustomerProfile(
                nickname,
                phone,
                CebuLocalities.gmailForName(nickname),
                score,
                CebuLocalities.addressForCustomer(nickname),
                "No additional notes on file.");
    }
}
