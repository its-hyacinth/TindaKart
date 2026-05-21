package com.ddev.TindaKart;

import java.awt.Font;
import java.text.DecimalFormat;

/** Philippine peso display (sign via \\u20B1 to avoid source-file encoding issues). */
public final class PesoFormat {

    public static final char SIGN_CHAR = '\u20B1';
    public static final String SIGN = String.valueOf(SIGN_CHAR);

    private static final DecimalFormat AMOUNT = new DecimalFormat("#,##0.00");

    private PesoFormat() {
    }

    public static String formatAmountOnly(double amount) {
        return AMOUNT.format(amount);
    }

    public static String format(double amount) {
        return SIGN + " " + formatAmountOnly(amount);
    }

    /** Font that can render the peso sign (Poppins does not). */
    public static Font font(int style, int size) {
        for (String family : new String[] { "Segoe UI Symbol", "Segoe UI", "Arial Unicode MS", Font.SANS_SERIF }) {
            Font candidate = new Font(family, style, size);
            if (candidate.canDisplay(SIGN_CHAR)) {
                return candidate;
            }
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    public static String stripForParse(String raw) {
        return raw.replace(SIGN, "").replace("P", "").replace(",", "").trim();
    }
}