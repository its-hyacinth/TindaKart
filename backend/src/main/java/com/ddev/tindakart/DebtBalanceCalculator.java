package com.ddev.tindakart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class DebtBalanceCalculator {
    public BigDecimal balance(BigDecimal totalCredit, BigDecimal totalPaid) {
        return money(totalCredit).subtract(money(totalPaid)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
