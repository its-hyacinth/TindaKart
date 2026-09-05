-- Give the role-specific staff accounts the permissions used by the operational APIs.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CASHIER' AND p.permission_key = 'POS_USE'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key IN ('INVENTORY_VIEW', 'INVENTORY_MANAGE')
WHERE r.name = 'INVENTORY_STAFF'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key = 'DEBT_MANAGE'
WHERE r.name = 'DEBT_STAFF'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.permission_key = 'DELIVERY_MANAGE'
WHERE r.name = 'DELIVERY_STAFF'
ON CONFLICT DO NOTHING;
