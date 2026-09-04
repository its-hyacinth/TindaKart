package com.ddev.tindakart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class PricingService {
    public BigDecimal unitPrice(BigDecimal retailPrice, BigDecimal bulkPrice, Integer bulkThreshold, BigDecimal quantity) {
        boolean bulk = bulkPrice != null && bulkThreshold != null
                && quantity.compareTo(BigDecimal.valueOf(bulkThreshold)) >= 0;
        return money(bulk ? bulkPrice : retailPrice);
    }

    public BigDecimal discountedSubtotal(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal result = money(subtotal).subtract(money(discount));
        if (result.signum() < 0) throw new IllegalArgumentException("Discount cannot exceed subtotal");
        return money(result);
    }

    public BigDecimal vat(BigDecimal taxableAmount, boolean vatRegistered, BigDecimal vatRate) {
        if (!vatRegistered) return BigDecimal.ZERO.setScale(2);
        return money(money(taxableAmount).multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
}
