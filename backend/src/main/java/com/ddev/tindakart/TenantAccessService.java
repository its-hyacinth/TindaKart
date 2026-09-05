package com.ddev.tindakart;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {
    private final JdbcTemplate jdbcTemplate;

    public TenantAccessService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<VendorAccess> vendorsFor(Authentication authentication) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) {
            return jdbcTemplate.query("SELECT id, name FROM vendors ORDER BY name",
                    (rs, rowNum) -> new VendorAccess(rs.getLong("id"), rs.getString("name")));
        }
        return jdbcTemplate.query("SELECT DISTINCT v.id, v.name FROM vendors v "
                + "JOIN (SELECT vendor_id, user_id FROM vendor_user_roles "
                + "UNION SELECT s.vendor_id, sur.user_id FROM store_user_roles sur "
                + "JOIN stores s ON s.id = sur.store_id) access ON access.vendor_id = v.id "
                + "JOIN users u ON u.id = access.user_id WHERE LOWER(u.username) = LOWER(?) ORDER BY v.name",
                (rs, rowNum) -> new VendorAccess(rs.getLong("id"), rs.getString("name")), authentication.getName());
    }

    public List<StoreAccess> storesFor(Authentication authentication) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) {
            return jdbcTemplate.query("SELECT id, vendor_id, name, code FROM stores ORDER BY name",
                    (rs, rowNum) -> store(rs));
        }
        return jdbcTemplate.query("SELECT DISTINCT s.id, s.vendor_id, s.name, s.code FROM stores s "
                + "WHERE EXISTS (SELECT 1 FROM vendor_user_roles vur JOIN users vu ON vu.id = vur.user_id "
                + "JOIN roles vr ON vr.id = vur.role_id WHERE vur.vendor_id = s.vendor_id "
                + "AND LOWER(vu.username) = LOWER(?) AND vr.name = 'VENDOR_ADMIN') "
                + "OR EXISTS (SELECT 1 FROM store_user_roles sur JOIN users su ON su.id = sur.user_id "
                + "WHERE sur.store_id = s.id AND LOWER(su.username) = LOWER(?)) ORDER BY s.name",
                (rs, rowNum) -> store(rs), authentication.getName(), authentication.getName());
    }

    public boolean hasVendorRole(Authentication authentication, Long vendorId, String roleName) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return true;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vendor_user_roles vur "
                + "JOIN users u ON u.id = vur.user_id JOIN roles r ON r.id = vur.role_id "
                + "WHERE vur.vendor_id = ? AND LOWER(u.username) = LOWER(?) AND r.name = ?",
                Integer.class, vendorId, authentication.getName(), roleName);
        return count != null && count > 0;
    }

    public boolean hasStoreAccess(Authentication authentication, Long storeId) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return true;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores s WHERE s.id = ? AND "
                + "(EXISTS (SELECT 1 FROM vendor_user_roles vur WHERE vur.vendor_id = s.vendor_id "
                + "AND vur.user_id = (SELECT id FROM users WHERE LOWER(username) = LOWER(?))) "
                + "OR EXISTS (SELECT 1 FROM store_user_roles sur WHERE sur.store_id = s.id "
                + "AND sur.user_id = (SELECT id FROM users WHERE LOWER(username) = LOWER(?))))",
                Integer.class, storeId, authentication.getName(), authentication.getName());
        return count != null && count > 0;
    }

    public boolean storeBelongsToVendor(Long vendorId, Long storeId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ? AND vendor_id = ?",
                Integer.class, storeId, vendorId);
        return count != null && count > 0;
    }

    public boolean hasStoreRole(Authentication authentication, Long storeId, String roleName) {
        if (hasRole(authentication, "ROLE_SUPER_ADMIN")) return true;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM store_user_roles sur "
                + "JOIN users u ON u.id = sur.user_id JOIN roles r ON r.id = sur.role_id "
                + "WHERE sur.store_id = ? AND LOWER(u.username) = LOWER(?) AND r.name = ?",
                Integer.class, storeId, authentication.getName(), roleName);
        return count != null && count > 0;
    }

    private StoreAccess store(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new StoreAccess(rs.getLong("id"), rs.getLong("vendor_id"), rs.getString("name"), rs.getString("code"));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    public record VendorAccess(Long id, String name) { }
    public record StoreAccess(Long id, Long vendorId, String name, String code) { }
}
