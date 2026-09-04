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
                + "JOIN store_user_roles sur ON sur.store_id = s.id "
                + "JOIN users u ON u.id = sur.user_id WHERE LOWER(u.username) = LOWER(?) ORDER BY s.name",
                (rs, rowNum) -> store(rs), authentication.getName());
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
