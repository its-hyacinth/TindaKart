package com.ddev.tindakart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingServiceTest {
    private final PricingService pricing = new PricingService();

    @Test
    void usesRetailPriceBelowBulkThreshold() {
        assertEquals(new BigDecimal("10.00"), pricing.unitPrice(new BigDecimal("10"), new BigDecimal("8"), 10, new BigDecimal("9")));
    }

    @Test
    void usesBulkPriceAtBulkThreshold() {
        assertEquals(new BigDecimal("8.00"), pricing.unitPrice(new BigDecimal("10"), new BigDecimal("8"), 10, new BigDecimal("10")));
    }

    @Test
    void rejectsDiscountAboveSubtotal() {
        assertThrows(IllegalArgumentException.class, () -> pricing.discountedSubtotal(new BigDecimal("10"), new BigDecimal("10.01")));
    }

    @Test
    void calculatesVatOnlyWhenRegistered() {
        assertEquals(new BigDecimal("12.00"), pricing.vat(new BigDecimal("100"), true, new BigDecimal("12")));
        assertEquals(new BigDecimal("0.00"), pricing.vat(new BigDecimal("100"), false, new BigDecimal("12")));
    }
}
