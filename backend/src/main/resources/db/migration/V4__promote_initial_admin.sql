-- Promote the existing demo-era admin account to the platform Super Admin.
-- This is intentionally a new migration because applied Flyway migrations
-- must not be edited.
DELETE FROM user_roles
WHERE user_id = (SELECT id FROM users WHERE LOWER(username) = 'admin')
  AND role_id = (SELECT id FROM roles WHERE name = 'VENDOR_ADMIN');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE LOWER(u.username) = 'admin'
  AND r.name = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
