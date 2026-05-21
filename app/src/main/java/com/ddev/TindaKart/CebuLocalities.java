package com.ddev.TindaKart;

import java.util.Locale;

/** Consolacion and Liloan, Cebu barangays for customer addresses. */
public final class CebuLocalities {

    public static final String CONSOLACION = "Consolacion, Cebu";
    public static final String LILOAN = "Liloan, Cebu";

    private static final String[] CONSOLACION_BARANGAYS = {
            "Cabangahan", "Cansaga", "Casili", "Danglag", "Garing", "Jugan", "Lamac", "Lanipga",
            "Nangka", "Panas", "Panoypoy", "Pitogo", "Poblacion Occidental", "Poblacion Oriental",
            "Polog", "Pulpogan", "Sacsac", "Tayud", "Tilhaong", "Tugbongan", "Tolotolo"
    };

    private static final String[] LILOAN_BARANGAYS = {
            "Cabadiangan", "Calero", "Catarman", "Cotcot", "Jubay", "Lataban", "Mulao", "Poblacion",
            "San Roque", "San Vicente", "Santa Cruz", "Tabla", "Tayud", "Yati"
    };

    private CebuLocalities() {
    }

    public static String formatAddress(String barangay, String municipality) {
        return "Brgy. " + barangay + ", " + municipality;
    }

    public static String consolacionAddress(int index) {
        return formatAddress(CONSOLACION_BARANGAYS[index % CONSOLACION_BARANGAYS.length], CONSOLACION);
    }

    public static String liloanAddress(int index) {
        return formatAddress(LILOAN_BARANGAYS[index % LILOAN_BARANGAYS.length], LILOAN);
    }

    /** Picks a barangay from Consolacion or Liloan based on a stable seed string. */
    public static String addressForCustomer(String seed) {
        int hash = Math.abs(seed.toLowerCase(Locale.ROOT).hashCode());
        boolean liloan = hash % 2 == 0;
        if (liloan) {
            return liloanAddress(hash);
        }
        return consolacionAddress(hash);
    }

    public static String gmailForName(String fullName) {
        String slug = fullName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.|\\.$", "");
        if (slug.isEmpty()) {
            slug = "customer";
        }
        return slug + "@gmail.com";
    }

    public static boolean isGmailAddress(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.endsWith("@gmail.com") && trimmed.length() > "@gmail.com".length();
    }
}
