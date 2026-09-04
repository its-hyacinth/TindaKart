CREATE UNIQUE INDEX subscription_payments_provider_payment_unique
    ON subscription_payments(provider, provider_payment_id)
    WHERE provider_payment_id IS NOT NULL;
