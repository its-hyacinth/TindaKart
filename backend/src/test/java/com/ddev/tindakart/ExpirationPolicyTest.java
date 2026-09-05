package com.ddev.tindakart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ExpirationPolicyTest {
    private final ExpirationPolicy policy = new ExpirationPolicy();
    private final LocalDate today = LocalDate.of(2026, 9, 5);

    @Test
    void marksPastDatesExpired() {
        assertEquals("EXPIRED", policy.status(today.minusDays(1), today, 30, new BigDecimal("100"), 5));
    }

    @Test
    void marksTodayAndConfiguredWindowNearExpiration() {
        assertEquals("NEAR_EXPIRATION", policy.status(today, today, 0, new BigDecimal("100"), 5));
        assertEquals("NEAR_EXPIRATION", policy.status(today.plusDays(30), today, 30, new BigDecimal("100"), 5));
    }

    @Test
    void usesLowStockWhenNoExpirationWarningApplies() {
        assertEquals("LOW_STOCK", policy.status(null, today, 30, new BigDecimal("5"), 5));
    }

    @Test
    void marksHealthyStockInStock() {
        assertEquals("IN_STOCK", policy.status(today.plusDays(31), today, 30, new BigDecimal("6"), 5));
    }
}
