-- Store-first administration foundation.
-- Existing operational tables remain available while their tenant columns are
-- migrated in a later, data-preserving migration.
INSERT INTO roles (name)
VALUES ('STORE_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT store_admin.id, rp.permission_id
FROM roles store_admin
JOIN roles previous_admin ON previous_admin.name = 'VENDOR_ADMIN'
JOIN role_permissions rp ON rp.role_id = previous_admin.id
WHERE store_admin.name = 'STORE_ADMIN'
ON CONFLICT DO NOTHING;

-- Give current administrators a store-scoped role for every store they
-- administer. This keeps existing accounts usable during the migration.
INSERT INTO store_user_roles (user_id, store_id, role_id)
SELECT vur.user_id, s.id, store_admin.id
FROM vendor_user_roles vur
JOIN stores s ON s.vendor_id = vur.vendor_id
JOIN roles previous_admin ON previous_admin.id = vur.role_id AND previous_admin.name = 'VENDOR_ADMIN'
JOIN roles store_admin ON store_admin.name = 'STORE_ADMIN'
ON CONFLICT DO NOTHING;
