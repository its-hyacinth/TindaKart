-- A destructive, store-first schema migration. Use the database reset script before
-- applying this migration in development; it intentionally removes the old parent
-- tenancy model and all dependent operational data.

DROP TABLE IF EXISTS vendors CASCADE;

CREATE TABLE IF NOT EXISTS stores (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    address VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stores_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE IF NOT EXISTS store_user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, store_id, role_id)
);

CREATE TABLE IF NOT EXISTS store_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    starts_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT store_subscriptions_status_check CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED', 'EXPIRED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS store_one_current_subscription
    ON store_subscriptions(store_id)
    WHERE status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED');

CREATE TABLE IF NOT EXISTS store_feature_addons (
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    feature_key VARCHAR(120) NOT NULL REFERENCES platform_addon_prices(feature_key) ON DELETE RESTRICT,
    monthly_price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (store_id, feature_key)
);

CREATE TABLE IF NOT EXISTS store_staff_seats (
    store_id BIGINT PRIMARY KEY REFERENCES stores(id) ON DELETE CASCADE,
    seat_count INTEGER NOT NULL DEFAULT 0,
    monthly_unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT store_staff_seats_nonnegative CHECK (seat_count >= 0 AND monthly_unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS subscription_checkout_sessions (
    id BIGSERIAL PRIMARY KEY,
    store_subscription_id BIGINT NOT NULL REFERENCES store_subscriptions(id) ON DELETE RESTRICT,
    provider VARCHAR(50) NOT NULL,
    provider_checkout_id VARCHAR(255) NOT NULL UNIQUE,
    checkout_url VARCHAR(1000) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PHP',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    purchase_type VARCHAR(40) NOT NULL DEFAULT 'PACKAGE',
    purchase_metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT checkout_sessions_amount_positive CHECK (amount > 0),
    CONSTRAINT checkout_sessions_status_check CHECK (status IN ('OPEN', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS subscription_events (
    id BIGSERIAL PRIMARY KEY,
    store_subscription_id BIGINT REFERENCES store_subscriptions(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload TEXT,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscription_events_provider_event_unique UNIQUE (provider, provider_event_id)
);

CREATE TABLE IF NOT EXISTS subscription_payments (
    id BIGSERIAL PRIMARY KEY,
    store_subscription_id BIGINT REFERENCES store_subscriptions(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    provider_payment_id VARCHAR(255),
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PHP',
    status VARCHAR(30) NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscription_payments_amount_positive CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS business_settings (
    store_id BIGINT PRIMARY KEY REFERENCES stores(id) ON DELETE CASCADE,
    business_name VARCHAR(255),
    business_address VARCHAR(500),
    tin VARCHAR(80),
    vat_registered BOOLEAN NOT NULL DEFAULT FALSE,
    vat_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    near_expiration_days INTEGER NOT NULL DEFAULT 30,
    receipt_footer VARCHAR(500),
    pos_payment_methods VARCHAR(30)[] NOT NULL DEFAULT ARRAY['CASH', 'CARD', 'EWALLET', 'CREDIT']::VARCHAR[],
    camera_scanning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    receipt_print_mode VARCHAR(20) NOT NULL DEFAULT 'BROWSER',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

UPDATE roles SET name = 'STORE_ADMIN' WHERE name IN ('OWNER', 'VENDOR_ADMIN');
INSERT INTO roles (name) VALUES ('STORE_ADMIN') ON CONFLICT (name) DO NOTHING;
