package com.ddev.tindakart;

import java.math.BigDecimal;

public interface SubscriptionPaymentProvider {
    boolean configured();
    Checkout createCheckout(String packageName, BigDecimal amount);
    record Checkout(String providerCheckoutId, String checkoutUrl) { }
}
