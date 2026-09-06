-- Platform defaults for a fresh TindaKart commercial setup.
-- Values are intentionally editable from the Super Admin portal after migration.

INSERT INTO packages (name, description, monthly_price, annual_price, active)
VALUES
    ('Free', 'Owner-operated store with POS and catalog access.', 0.00, 0.00, TRUE),
    ('Growth', 'For growing stores that need inventory, reporting, and a small team.', 999.00, 9990.00, TRUE),
    ('Pro', 'For multi-store operations with larger teams and full platform access.', 1999.00, 19990.00, TRUE)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    monthly_price = EXCLUDED.monthly_price,
    annual_price = EXCLUDED.annual_price,
    active = TRUE;

INSERT INTO package_features (package_id, feature_key, enabled)
SELECT p.id, feature.feature_key, TRUE
FROM packages p
CROSS JOIN (VALUES
    ('POS'), ('CATALOG'), ('INVENTORY'), ('DEBT'), ('DELIVERY'), ('REPORTS'), ('STAFF_MANAGEMENT'), ('BARCODE_SCANNING')
) AS feature(feature_key)
WHERE p.name IN ('Growth', 'Pro')
ON CONFLICT (package_id, feature_key) DO UPDATE SET enabled = TRUE;

INSERT INTO package_features (package_id, feature_key, enabled)
SELECT p.id, feature.feature_key, TRUE
FROM packages p
CROSS JOIN (VALUES ('POS'), ('CATALOG')) AS feature(feature_key)
WHERE p.name = 'Free'
ON CONFLICT (package_id, feature_key) DO UPDATE SET enabled = TRUE;

INSERT INTO package_limits (package_id, limit_key, limit_value)
SELECT p.id, limits.limit_key, limits.limit_value
FROM packages p
JOIN (VALUES
    ('Free', 'MAX_STORES', 1), ('Free', 'MAX_STAFF', 0), ('Free', 'MAX_PRODUCTS', 100),
    ('Growth', 'MAX_STORES', 3), ('Growth', 'MAX_STAFF', 5), ('Growth', 'MAX_PRODUCTS', 2500),
    ('Pro', 'MAX_STORES', 100000), ('Pro', 'MAX_STAFF', 100000), ('Pro', 'MAX_PRODUCTS', 100000)
) AS limits(package_name, limit_key, limit_value) ON limits.package_name = p.name
ON CONFLICT (package_id, limit_key) DO UPDATE SET limit_value = EXCLUDED.limit_value;

INSERT INTO platform_addon_prices (feature_key, display_name, description, monthly_price, active)
VALUES
    ('STAFF_SEAT', 'Additional staff seat', 'One additional vendor staff account beyond the package allowance.', 149.00, TRUE),
    ('INVENTORY', 'Inventory', 'Stock receiving, adjustments, movement history, and alerts.', 199.00, TRUE),
    ('CATALOG', 'Catalog', 'Products, categories, pricing, and barcode management.', 99.00, TRUE),
    ('DEBT', 'Debt and credit', 'Customer credit accounts and payment history.', 149.00, TRUE),
    ('DELIVERY', 'Deliveries', 'Supplier deliveries and receiving workflow.', 149.00, TRUE),
    ('REPORTS', 'Reports', 'Sales, stock, expiration, tax, and payment reports.', 199.00, TRUE),
    ('BARCODE_SCANNING', 'Barcode scanning', 'Barcode scanner and camera-assisted product lookup.', 99.00, TRUE)
ON CONFLICT (feature_key) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    monthly_price = EXCLUDED.monthly_price,
    active = TRUE;
