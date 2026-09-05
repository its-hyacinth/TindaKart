package com.ddev.tindakart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DebtBalanceCalculatorTest {
    private final DebtBalanceCalculator calculator = new DebtBalanceCalculator();

    @Test
    void calculatesOutstandingBalance() {
        assertEquals(new BigDecimal("75.00"), calculator.balance(new BigDecimal("100"), new BigDecimal("25")));
    }

    @Test
    void normalizesNullAmountsToZero() {
        assertEquals(new BigDecimal("100.00"), calculator.balance(new BigDecimal("100"), null));
    }

    @Test
    void preservesOverpaymentAsNegativeForCallerValidation() {
        assertEquals(new BigDecimal("-10.00"), calculator.balance(new BigDecimal("100"), new BigDecimal("110")));
    }
}
