CREATE TABLE subscription_checkout_sessions (
    id BIGSERIAL PRIMARY KEY,
    vendor_subscription_id BIGINT NOT NULL REFERENCES vendor_subscriptions(id) ON DELETE RESTRICT,
    provider VARCHAR(50) NOT NULL,
    provider_checkout_id VARCHAR(255) NOT NULL UNIQUE,
    checkout_url VARCHAR(1000) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PHP',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT checkout_sessions_amount_positive CHECK (amount > 0),
    CONSTRAINT checkout_sessions_status_check CHECK (status IN ('OPEN', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED'))
);
