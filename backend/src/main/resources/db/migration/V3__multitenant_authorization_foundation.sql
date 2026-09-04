CREATE TABLE vendors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT vendors_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CANCELLED'))
);

CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(80) NOT NULL,
    address VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT stores_vendor_code_unique UNIQUE (vendor_id, code),
    CONSTRAINT stores_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_key VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE vendor_user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, vendor_id, role_id)
);

CREATE TABLE store_user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, store_id, role_id)
);

CREATE TABLE packages (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    monthly_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    annual_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT packages_prices_nonnegative CHECK (monthly_price >= 0 AND annual_price >= 0)
);

CREATE TABLE package_features (
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    feature_key VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (package_id, feature_key)
);

CREATE TABLE package_limits (
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    limit_key VARCHAR(120) NOT NULL,
    limit_value INTEGER NOT NULL,
    PRIMARY KEY (package_id, limit_key),
    CONSTRAINT package_limits_nonnegative CHECK (limit_value >= 0)
);

CREATE TABLE vendor_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    starts_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMPTZ,
    provider VARCHAR(50),
    provider_subscription_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscriptions_status_check CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED', 'EXPIRED'))
);

CREATE UNIQUE INDEX vendor_one_current_subscription
    ON vendor_subscriptions(vendor_id)
    WHERE status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED');

CREATE TABLE subscription_events (
    id BIGSERIAL PRIMARY KEY,
    vendor_subscription_id BIGINT REFERENCES vendor_subscriptions(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload TEXT,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscription_events_provider_event_unique UNIQUE (provider, provider_event_id)
);

CREATE TABLE subscription_payments (
    id BIGSERIAL PRIMARY KEY,
    vendor_subscription_id BIGINT REFERENCES vendor_subscriptions(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    provider_payment_id VARCHAR(255),
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'PHP',
    status VARCHAR(30) NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT subscription_payments_amount_positive CHECK (amount >= 0)
);

-- Rename the demo-era role to the business role before adding new roles.
UPDATE roles SET name = 'VENDOR_ADMIN' WHERE name = 'OWNER';
INSERT INTO roles (name) VALUES ('SUPER_ADMIN'), ('STAFF'), ('CASHIER'), ('INVENTORY_STAFF'), ('DEBT_STAFF'), ('DELIVERY_STAFF')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (permission_key, description) VALUES
    ('PLATFORM_VENDOR_MANAGE', 'Manage vendors on the TindaKart platform'),
    ('PLATFORM_PACKAGE_MANAGE', 'Manage packages, features, and limits'),
    ('PLATFORM_BILLING_MANAGE', 'Manage platform subscription billing'),
    ('VENDOR_STORE_MANAGE', 'Manage vendor stores and branches'),
    ('VENDOR_STAFF_MANAGE', 'Manage vendor staff accounts'),
    ('CATALOG_MANAGE', 'Manage products and categories'),
    ('INVENTORY_VIEW', 'View inventory'),
    ('INVENTORY_MANAGE', 'Receive and adjust inventory'),
    ('POS_USE', 'Use the point of sale'),
    ('DEBT_MANAGE', 'Manage debt and credit records'),
    ('DELIVERY_MANAGE', 'Manage deliveries'),
    ('REPORTS_VIEW', 'View operational reports')
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key IN (
    'VENDOR_STORE_MANAGE', 'VENDOR_STAFF_MANAGE', 'CATALOG_MANAGE', 'INVENTORY_VIEW',
    'INVENTORY_MANAGE', 'POS_USE', 'DEBT_MANAGE', 'DELIVERY_MANAGE', 'REPORTS_VIEW')
WHERE r.name = 'VENDOR_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key IN ('POS_USE', 'INVENTORY_VIEW')
WHERE r.name = 'STAFF'
ON CONFLICT DO NOTHING;
