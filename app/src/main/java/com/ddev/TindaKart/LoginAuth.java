package com.ddev.TindaKart;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pre-assigned application logins for demo / capstone use.
 */
public final class LoginAuth {

    private static final Map<String, String> PASSWORDS;
    private static final Map<String, String> DISPLAY_NAMES;

    static {
        Map<String, String> passwords = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();

        register(passwords, names, "Myrna Abucay", "myrna123");
        register(passwords, names, "Jana Aldiano", "jana123");
        register(passwords, names, "Arvin Wagas", "arvin123");
        register(passwords, names, "Mona Canillo", "mona123");

        PASSWORDS = Collections.unmodifiableMap(passwords);
        DISPLAY_NAMES = Collections.unmodifiableMap(names);
    }

    private LoginAuth() {
    }

    private static void register(Map<String, String> passwords, Map<String, String> names,
            String fullName, String password) {
        String key = normalize(fullName);
        passwords.put(key, password);
        names.put(key, fullName);
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        String expected = PASSWORDS.get(normalize(username));
        return expected != null && expected.equals(password);
    }

    public static Optional<String> displayNameFor(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(DISPLAY_NAMES.get(normalize(username)));
    }

    public static String demoAccountsText() {
        return String.join("\n", DISPLAY_NAMES.values());
    }

    public static String passwordHintText() {
        return "Password: first name + 123 (example: myrna123)";
    }
}
