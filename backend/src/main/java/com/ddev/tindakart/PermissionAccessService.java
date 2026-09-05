package com.ddev.tindakart;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PermissionAccessService {
    private final JdbcTemplate jdbcTemplate;

    public PermissionAccessService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void require(Authentication authentication, Long vendorId, Long storeId, String permission) {
        if (!has(authentication, vendorId, storeId, permission)) {
            throw new AccessDeniedException("Permission is required: " + permission);
        }
    }

    public boolean has(Authentication authentication, Long vendorId, Long storeId, String permission) {
        if (hasAuthority(authentication, "ROLE_SUPER_ADMIN")) return true;
        if (vendorId == null || storeId == null) return false;
        Integer allowed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores s "
                        + "JOIN ("
                        + "SELECT vur.user_id, vur.vendor_id, vur.role_id FROM vendor_user_roles vur "
                        + "UNION ALL "
                        + "SELECT sur.user_id, st.vendor_id, sur.role_id FROM store_user_roles sur JOIN stores st ON st.id = sur.store_id"
                        + ") assignments ON assignments.vendor_id = s.vendor_id "
                        + "JOIN users u ON u.id = assignments.user_id "
                        + "JOIN role_permissions rp ON rp.role_id = assignments.role_id "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "WHERE s.id = ? AND s.vendor_id = ? AND LOWER(u.username) = LOWER(?) AND p.permission_key = ?",
                Integer.class, storeId, vendorId, authentication.getName(), permission);
        return allowed != null && allowed > 0;
    }

    public void requireVendor(Authentication authentication, Long vendorId, String permission) {
        if (hasAuthority(authentication, "ROLE_SUPER_ADMIN")) return;
        Integer allowed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ("
                        + "SELECT vur.user_id, vur.vendor_id, vur.role_id FROM vendor_user_roles vur "
                        + "UNION ALL SELECT sur.user_id, st.vendor_id, sur.role_id FROM store_user_roles sur JOIN stores st ON st.id = sur.store_id"
                        + ") assignments JOIN users u ON u.id = assignments.user_id "
                        + "JOIN role_permissions rp ON rp.role_id = assignments.role_id JOIN permissions p ON p.id = rp.permission_id "
                        + "WHERE assignments.vendor_id = ? AND LOWER(u.username) = LOWER(?) AND p.permission_key = ?",
                Integer.class, vendorId, authentication.getName(), permission);
        if (allowed == null || allowed == 0) throw new AccessDeniedException("Permission is required: " + permission);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
