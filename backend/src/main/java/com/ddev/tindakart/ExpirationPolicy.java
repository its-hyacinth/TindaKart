package com.ddev.tindakart;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class ExpirationPolicy {
    public String status(LocalDate expirationDate, LocalDate today, int nearExpirationDays,
                         BigDecimal quantityOnHand, int reorderLevel) {
        if (expirationDate != null && expirationDate.isBefore(today)) return "EXPIRED";
        if (expirationDate != null
                && ChronoUnit.DAYS.between(today, expirationDate) <= Math.max(0, nearExpirationDays)) {
            return "NEAR_EXPIRATION";
        }
        return quantityOnHand.compareTo(BigDecimal.valueOf(reorderLevel)) <= 0 ? "LOW_STOCK" : "IN_STOCK";
    }
}
