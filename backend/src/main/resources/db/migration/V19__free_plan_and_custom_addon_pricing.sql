ALTER TABLE vendors ADD COLUMN IF NOT EXISTS owner_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS platform_addon_prices (
    feature_key VARCHAR(120) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    monthly_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT platform_addon_prices_nonnegative CHECK (monthly_price >= 0)
);

CREATE TABLE IF NOT EXISTS vendor_feature_addons (
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    feature_key VARCHAR(120) NOT NULL REFERENCES platform_addon_prices(feature_key) ON DELETE RESTRICT,
    monthly_price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (vendor_id, feature_key),
    CONSTRAINT vendor_feature_addons_nonnegative CHECK (monthly_price >= 0)
);

CREATE TABLE IF NOT EXISTS vendor_staff_seats (
    vendor_id BIGINT PRIMARY KEY REFERENCES vendors(id) ON DELETE CASCADE,
    seat_count INTEGER NOT NULL DEFAULT 0,
    monthly_unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT vendor_staff_seats_nonnegative CHECK (seat_count >= 0 AND monthly_unit_price >= 0)
);

ALTER TABLE subscription_checkout_sessions ADD COLUMN IF NOT EXISTS purchase_type VARCHAR(40) NOT NULL DEFAULT 'PACKAGE';
ALTER TABLE subscription_checkout_sessions ADD COLUMN IF NOT EXISTS purchase_metadata TEXT;

INSERT INTO packages (name, description, monthly_price, annual_price, active)
VALUES ('Free', 'Owner-operated store with no additional staff seats.', 0, 0, TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO package_features (package_id, feature_key, enabled)
SELECT p.id, controls.feature_key, TRUE
FROM packages p
CROSS JOIN (VALUES ('POS'), ('INVENTORY'), ('CATALOG'), ('DEBT'), ('DELIVERY'), ('REPORTS')) AS controls(feature_key)
WHERE p.name = 'Free'
ON CONFLICT DO NOTHING;

INSERT INTO package_limits (package_id, limit_key, limit_value)
SELECT p.id, controls.limit_key, controls.limit_value
FROM packages p
CROSS JOIN (VALUES ('MAX_STORES', 1), ('MAX_STAFF', 0), ('MAX_PRODUCTS', 100)) AS controls(limit_key, limit_value)
WHERE p.name = 'Free'
ON CONFLICT DO NOTHING;

INSERT INTO platform_addon_prices (feature_key, display_name, description, monthly_price)
VALUES
    ('STAFF_SEAT', 'Additional staff seat', 'One additional vendor staff account beyond the owner.', 0),
    ('INVENTORY', 'Inventory', 'Stock receiving, adjustments, movement history, and alerts.', 0),
    ('CATALOG', 'Catalog', 'Products, categories, pricing, and barcodes.', 0),
    ('DEBT', 'Debt and credit', 'Customer credit accounts and payment history.', 0),
    ('DELIVERY', 'Deliveries', 'Supplier deliveries and receiving workflow.', 0),
    ('REPORTS', 'Reports', 'Sales, stock, expiration, tax, and payment reports.', 0)
ON CONFLICT (feature_key) DO NOTHING;

UPDATE vendors v
SET owner_user_id = owners.user_id
FROM (
    SELECT DISTINCT ON (vur.vendor_id) vur.vendor_id, vur.user_id
    FROM vendor_user_roles vur
    JOIN roles r ON r.id = vur.role_id
    WHERE r.name = 'VENDOR_ADMIN'
    ORDER BY vur.vendor_id, vur.user_id
) owners
WHERE v.id = owners.vendor_id AND v.owner_user_id IS NULL;

INSERT INTO vendor_staff_seats (vendor_id, seat_count, monthly_unit_price)
SELECT v.id, 0, 0 FROM vendors v
ON CONFLICT (vendor_id) DO NOTHING;
