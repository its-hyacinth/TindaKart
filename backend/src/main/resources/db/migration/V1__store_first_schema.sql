CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX audit_logs_action_created_idx ON audit_logs(action, created_at DESC);

CREATE TABLE login_attempts (
    username VARCHAR(120) PRIMARY KEY,
    failed_count INTEGER NOT NULL DEFAULT 0,
    first_failed_at TIMESTAMPTZ,
    locked_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    address VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

CREATE TABLE platform_addon_prices (
    feature_key VARCHAR(120) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    monthly_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT platform_addon_prices_nonnegative CHECK (monthly_price >= 0)
);

CREATE TABLE store_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    package_id BIGINT NOT NULL REFERENCES packages(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    starts_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT store_subscriptions_status_check CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED', 'EXPIRED'))
);

CREATE UNIQUE INDEX store_one_current_subscription ON store_subscriptions(store_id)
    WHERE status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED');

CREATE TABLE store_feature_addons (
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    feature_key VARCHAR(120) NOT NULL REFERENCES platform_addon_prices(feature_key) ON DELETE RESTRICT,
    monthly_price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (store_id, feature_key),
    CONSTRAINT store_feature_addons_nonnegative CHECK (monthly_price >= 0)
);

CREATE TABLE store_staff_seats (
    store_id BIGINT PRIMARY KEY REFERENCES stores(id) ON DELETE CASCADE,
    seat_count INTEGER NOT NULL DEFAULT 0,
    monthly_unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT store_staff_seats_nonnegative CHECK (seat_count >= 0 AND monthly_unit_price >= 0)
);

CREATE TABLE subscription_checkout_sessions (
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

CREATE TABLE subscription_events (
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

CREATE TABLE subscription_payments (
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

CREATE UNIQUE INDEX subscription_payments_provider_payment_unique
    ON subscription_payments(provider, provider_payment_id) WHERE provider_payment_id IS NOT NULL;

CREATE TABLE business_settings (
    store_id BIGINT PRIMARY KEY REFERENCES stores(id) ON DELETE CASCADE,
    business_name VARCHAR(255),
    business_address VARCHAR(500),
    tin VARCHAR(80),
    vat_registered BOOLEAN NOT NULL DEFAULT FALSE,
    vat_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    near_expiration_days INTEGER NOT NULL DEFAULT 30,
    near_expiration_discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    receipt_footer VARCHAR(500),
    pos_payment_methods VARCHAR(30)[] NOT NULL DEFAULT ARRAY['CASH', 'CARD', 'EWALLET', 'CREDIT']::VARCHAR[],
    camera_scanning_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    receipt_print_mode VARCHAR(20) NOT NULL DEFAULT 'BROWSER',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT business_settings_vat_check CHECK (vat_rate BETWEEN 0 AND 100),
    CONSTRAINT business_settings_expiration_check CHECK (near_expiration_days >= 0),
    CONSTRAINT business_settings_expiration_discount_check CHECK (near_expiration_discount_percent BETWEEN 0 AND 100),
    CONSTRAINT business_settings_payment_methods_check CHECK (cardinality(pos_payment_methods) > 0),
    CONSTRAINT business_settings_print_mode_check CHECK (receipt_print_mode IN ('BROWSER', 'THERMAL_BRIDGE', 'MANUAL'))
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT categories_store_name_unique UNIQUE (store_id, name)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(120) NOT NULL,
    unit_type VARCHAR(30) NOT NULL DEFAULT 'PIECE',
    cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    retail_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    bulk_price NUMERIC(12, 2),
    bulk_threshold INTEGER,
    reorder_level INTEGER NOT NULL DEFAULT 0,
    expiration_applicable BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_store_sku_unique UNIQUE (store_id, sku),
    CONSTRAINT products_unit_type_check CHECK (unit_type IN ('PIECE', 'PACK', 'BOTTLE', 'KILOGRAM', 'LITER', 'OTHER')),
    CONSTRAINT products_prices_nonnegative CHECK (cost_price >= 0 AND retail_price >= 0 AND (bulk_price IS NULL OR bulk_price >= 0)),
    CONSTRAINT products_bulk_fields_check CHECK ((bulk_price IS NULL AND bulk_threshold IS NULL) OR (bulk_price IS NOT NULL AND bulk_threshold > 0)),
    CONSTRAINT products_reorder_nonnegative CHECK (reorder_level >= 0)
);

CREATE TABLE product_barcodes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    barcode VARCHAR(120) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX products_store_name_idx ON products(store_id, name);
CREATE INDEX product_barcodes_product_idx ON product_barcodes(product_id);

CREATE TABLE inventory_batches (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_reference VARCHAR(120),
    quantity_received NUMERIC(12, 3) NOT NULL,
    quantity_on_hand NUMERIC(12, 3) NOT NULL,
    cost_price NUMERIC(12, 2) NOT NULL,
    retail_price NUMERIC(12, 2),
    bulk_price NUMERIC(12, 2),
    expiration_date DATE,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_batches_quantities_check CHECK (quantity_received > 0 AND quantity_on_hand BETWEEN 0 AND quantity_received),
    CONSTRAINT inventory_batches_prices_check CHECK (cost_price >= 0 AND (retail_price IS NULL OR retail_price >= 0) AND (bulk_price IS NULL OR bulk_price >= 0))
);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_id BIGINT REFERENCES inventory_batches(id) ON DELETE SET NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity_delta NUMERIC(12, 3) NOT NULL,
    reason VARCHAR(500),
    reference_type VARCHAR(50),
    reference_id VARCHAR(120),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_movements_type_check CHECK (movement_type IN ('RECEIVE', 'SALE', 'ADJUSTMENT', 'RETURN', 'VOID')),
    CONSTRAINT inventory_movements_delta_check CHECK (quantity_delta <> 0)
);

CREATE INDEX inventory_batches_store_product_idx ON inventory_batches(store_id, product_id);
CREATE INDEX inventory_batches_expiration_idx ON inventory_batches(expiration_date);
CREATE INDEX inventory_movements_store_product_idx ON inventory_movements(store_id, product_id, created_at);

CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    cashier_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    receipt_number VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    subtotal NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    vat_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_status_check CHECK (status IN ('COMPLETED', 'VOIDED')),
    CONSTRAINT sales_amounts_check CHECK (subtotal >= 0 AND discount_amount >= 0 AND vat_amount >= 0 AND total_amount >= 0)
);

CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_id BIGINT NOT NULL REFERENCES inventory_batches(id) ON DELETE RESTRICT,
    quantity NUMERIC(12, 3) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(12, 2) NOT NULL,
    CONSTRAINT sale_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT sale_items_amounts_check CHECK (unit_price >= 0 AND discount_amount >= 0 AND subtotal >= 0)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
    payment_method VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    amount_tendered NUMERIC(12, 2),
    change_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payments_method_check CHECK (payment_method IN ('CASH', 'CARD', 'EWALLET', 'CREDIT')),
    CONSTRAINT payments_amount_check CHECK (amount > 0 AND change_amount >= 0)
);

CREATE TABLE receipts (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL UNIQUE REFERENCES sales(id) ON DELETE RESTRICT,
    receipt_number VARCHAR(80) NOT NULL UNIQUE,
    printed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX sales_store_created_idx ON sales(store_id, created_at);
CREATE INDEX sale_items_product_idx ON sale_items(product_id);

CREATE TABLE customer_profiles (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(40),
    address VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE debt_accounts (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customer_profiles(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT debt_accounts_status_check CHECK (status IN ('OPEN', 'PAID', 'OVERDUE')),
    CONSTRAINT debt_accounts_customer_unique UNIQUE (store_id, customer_id)
);

CREATE TABLE credit_sales (
    id BIGSERIAL PRIMARY KEY,
    debt_account_id BIGINT NOT NULL REFERENCES debt_accounts(id) ON DELETE RESTRICT,
    sale_id BIGINT NOT NULL UNIQUE REFERENCES sales(id) ON DELETE RESTRICT,
    principal_amount NUMERIC(12, 2) NOT NULL,
    due_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT credit_sales_amount_positive CHECK (principal_amount > 0)
);

CREATE SEQUENCE debt_payment_receipt_seq START WITH 1;

CREATE TABLE debt_payments (
    id BIGSERIAL PRIMARY KEY,
    debt_account_id BIGINT NOT NULL REFERENCES debt_accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH',
    notes VARCHAR(500),
    receipt_number VARCHAR(80),
    paid_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT debt_payments_amount_positive CHECK (amount > 0)
);

CREATE TABLE debt_payment_allocations (
    payment_id BIGINT NOT NULL REFERENCES debt_payments(id) ON DELETE CASCADE,
    credit_sale_id BIGINT NOT NULL REFERENCES credit_sales(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL,
    PRIMARY KEY (payment_id, credit_sale_id),
    CONSTRAINT debt_allocations_amount_positive CHECK (amount > 0)
);

CREATE INDEX credit_sales_account_due_idx ON credit_sales(debt_account_id, due_date);
CREATE INDEX debt_payments_account_date_idx ON debt_payments(debt_account_id, paid_at);
CREATE UNIQUE INDEX debt_payments_receipt_number_idx ON debt_payments(receipt_number) WHERE receipt_number IS NOT NULL;

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(40),
    address VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT suppliers_store_name_unique UNIQUE (store_id, name)
);

CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    expected_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT deliveries_status_check CHECK (status IN ('UPCOMING', 'IN_TRANSIT', 'RECEIVED', 'COMPLETED'))
);

CREATE TABLE delivery_items (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity_ordered NUMERIC(12, 3) NOT NULL,
    quantity_received NUMERIC(12, 3) NOT NULL DEFAULT 0,
    quantity_missing NUMERIC(12, 3) NOT NULL DEFAULT 0,
    quantity_damaged NUMERIC(12, 3) NOT NULL DEFAULT 0,
    cost_price NUMERIC(12, 2) NOT NULL,
    retail_price NUMERIC(12, 2),
    bulk_price NUMERIC(12, 2),
    expiration_date DATE,
    batch_reference VARCHAR(120),
    CONSTRAINT delivery_items_quantity_check CHECK (quantity_ordered > 0 AND quantity_received >= 0 AND quantity_received <= quantity_ordered),
    CONSTRAINT delivery_items_receiving_quantities_check CHECK (quantity_received + quantity_missing + quantity_damaged <= quantity_ordered AND quantity_missing >= 0 AND quantity_damaged >= 0),
    CONSTRAINT delivery_items_cost_check CHECK (cost_price >= 0)
);

CREATE INDEX deliveries_store_date_idx ON deliveries(store_id, expected_date);
CREATE INDEX delivery_items_delivery_idx ON delivery_items(delivery_id);

INSERT INTO roles(name) VALUES
    ('SUPER_ADMIN'), ('STORE_ADMIN'), ('STAFF'), ('CASHIER'),
    ('INVENTORY_STAFF'), ('DEBT_STAFF'), ('DELIVERY_STAFF')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions(permission_key, description) VALUES
    ('PLATFORM_STORE_MANAGE', 'Manage stores on the TindaKart platform'),
    ('PLATFORM_PACKAGE_MANAGE', 'Manage packages, features, and limits'),
    ('PLATFORM_BILLING_MANAGE', 'Manage platform subscription billing'),
    ('STORE_STAFF_MANAGE', 'Manage store staff accounts'),
    ('CATALOG_MANAGE', 'Manage products and categories'),
    ('INVENTORY_VIEW', 'View inventory'),
    ('INVENTORY_MANAGE', 'Receive and adjust inventory'),
    ('POS_USE', 'Use the point of sale'),
    ('DEBT_MANAGE', 'Manage debt and credit records'),
    ('DELIVERY_MANAGE', 'Manage deliveries'),
    ('REPORTS_VIEW', 'View operational reports')
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'STORE_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key IN ('POS_USE', 'INVENTORY_VIEW') WHERE r.name = 'STAFF'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key = 'POS_USE' WHERE r.name = 'CASHIER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key IN ('INVENTORY_VIEW', 'INVENTORY_MANAGE') WHERE r.name = 'INVENTORY_STAFF'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key = 'DEBT_MANAGE' WHERE r.name = 'DEBT_STAFF'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key = 'DELIVERY_MANAGE' WHERE r.name = 'DELIVERY_STAFF'
ON CONFLICT DO NOTHING;

INSERT INTO packages(name, description, monthly_price, annual_price, active) VALUES
    ('Free', 'Owner-operated store with POS and catalog access.', 0.00, 0.00, TRUE),
    ('Growth', 'For growing stores that need inventory, reporting, and a small team.', 999.00, 9990.00, TRUE),
    ('Pro', 'For larger teams that need full platform access.', 1999.00, 19990.00, TRUE);

INSERT INTO package_features(package_id, feature_key, enabled)
SELECT p.id, f.feature_key, TRUE FROM packages p
CROSS JOIN (VALUES ('POS'), ('CATALOG')) AS f(feature_key) WHERE p.name = 'Free';

INSERT INTO package_features(package_id, feature_key, enabled)
SELECT p.id, f.feature_key, TRUE FROM packages p
CROSS JOIN (VALUES ('POS'), ('CATALOG'), ('INVENTORY'), ('DEBT'), ('DELIVERY'), ('REPORTS'), ('STAFF_MANAGEMENT'), ('BARCODE_SCANNING')) AS f(feature_key)
WHERE p.name IN ('Growth', 'Pro');

INSERT INTO package_limits(package_id, limit_key, limit_value)
SELECT p.id, l.limit_key, l.limit_value FROM packages p JOIN (VALUES
    ('Free', 'MAX_STORES', 1), ('Free', 'MAX_STAFF', 0), ('Free', 'MAX_PRODUCTS', 100),
    ('Growth', 'MAX_STORES', 1), ('Growth', 'MAX_STAFF', 5), ('Growth', 'MAX_PRODUCTS', 2500),
    ('Pro', 'MAX_STORES', 1), ('Pro', 'MAX_STAFF', 100000), ('Pro', 'MAX_PRODUCTS', 100000)
) AS l(package_name, limit_key, limit_value) ON l.package_name = p.name;

INSERT INTO platform_addon_prices(feature_key, display_name, description, monthly_price) VALUES
    ('STAFF_SEAT', 'Additional staff seat', 'One additional store staff account beyond the package allowance.', 149.00),
    ('INVENTORY', 'Inventory', 'Stock receiving, adjustments, movement history, and alerts.', 199.00),
    ('CATALOG', 'Catalog', 'Products, categories, pricing, and barcode management.', 99.00),
    ('DEBT', 'Debt and credit', 'Customer credit accounts and payment history.', 149.00),
    ('DELIVERY', 'Deliveries', 'Supplier deliveries and receiving workflow.', 149.00),
    ('REPORTS', 'Reports', 'Sales, stock, expiration, tax, and payment reports.', 199.00),
    ('BARCODE_SCANNING', 'Barcode scanning', 'Barcode scanner and camera-assisted product lookup.', 99.00);
